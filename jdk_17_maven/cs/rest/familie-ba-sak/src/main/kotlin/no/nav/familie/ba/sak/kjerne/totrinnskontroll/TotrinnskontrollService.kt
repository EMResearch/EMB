package no.nav.familie.ba.sak.kjerne.totrinnskontroll

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(
    private val behandlingService: BehandlingService,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val saksbehandlerContext: SaksbehandlerContext,
) {

    fun hentAktivForBehandling(behandlingId: Long): Totrinnskontroll? {
        return totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun opprettTotrinnskontrollMedSaksbehandler(
        behandling: Behandling,
        saksbehandler: String = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(),
        saksbehandlerId: String = SikkerhetContext.hentSaksbehandler(),
    ): Totrinnskontroll {
        return lagreOgDeaktiverGammel(
            Totrinnskontroll(
                behandling = behandling,
                saksbehandler = saksbehandler,
                saksbehandlerId = saksbehandlerId,
            ),
        )
    }

    fun besluttTotrinnskontroll(
        behandling: Behandling,
        beslutter: String,
        beslutterId: String,
        beslutning: Beslutning,
        kontrollerteSider: List<String> = emptyList(),
    ): Totrinnskontroll {
        val totrinnskontroll = hentAktivForBehandling(behandlingId = behandling.id)
            ?: throw Feil(message = "Kan ikke beslutte et vedtak som ikke er sendt til beslutter")

        totrinnskontroll.beslutter = beslutter
        totrinnskontroll.beslutterId = beslutterId
        totrinnskontroll.godkjent = beslutning.erGodkjent()
        totrinnskontroll.kontrollerteSider = kontrollerteSider
        if (totrinnskontroll.erUgyldig()) {
            throw FunksjonellFeil(
                melding = "Samme saksbehandler kan ikke foreslå og beslutte iverksetting på samme vedtak",
                frontendFeilmelding = "Du kan ikke godkjenne ditt eget vedtak",
            )
        }

        lagreEllerOppdater(totrinnskontroll)

        behandlingService.oppdaterStatusPåBehandling(
            behandlingId = behandling.id,
            status = if (beslutning.erGodkjent()) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES,
        )

        return totrinnskontroll
    }

    fun opprettAutomatiskTotrinnskontroll(behandling: Behandling) {
        if (!behandling.skalBehandlesAutomatisk) {
            throw Feil(message = "Kan ikke opprette automatisk totrinnskontroll ved manuell behandling")
        }

        lagreOgDeaktiverGammel(
            Totrinnskontroll(
                behandling = behandling,
                godkjent = true,
                saksbehandler = SikkerhetContext.SYSTEM_NAVN,
                saksbehandlerId = SikkerhetContext.SYSTEM_FORKORTELSE,
                beslutter = SikkerhetContext.SYSTEM_NAVN,
                beslutterId = SikkerhetContext.SYSTEM_FORKORTELSE,
            ),
        )
    }

    fun lagreOgDeaktiverGammel(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        val aktivTotrinnskontroll = hentAktivForBehandling(totrinnskontroll.behandling.id)

        if (aktivTotrinnskontroll != null && aktivTotrinnskontroll.id != totrinnskontroll.id) {
            totrinnskontrollRepository.saveAndFlush(aktivTotrinnskontroll.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter totrinnskontroll $totrinnskontroll")
        return totrinnskontrollRepository.save(totrinnskontroll)
    }

    fun lagreEllerOppdater(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        return totrinnskontrollRepository.save(totrinnskontroll)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TotrinnskontrollService::class.java)
    }
}
