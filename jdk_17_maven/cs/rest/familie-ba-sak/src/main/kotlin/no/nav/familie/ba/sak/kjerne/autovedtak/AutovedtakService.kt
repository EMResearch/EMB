package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutovedtakService(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val loggService: LoggService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
) {
    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        aktør: Aktør,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
        fagsakId: Long,
    ): Behandling {
        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                søkersIdent = aktør.aktivFødselsnummer(),
                skalBehandlesAutomatisk = true,
                fagsakId = fagsakId,
            ),
        )

        val behandlingEtterBehandlingsresultat = stegService.håndterVilkårsvurdering(nyBehandling)
        return behandlingEtterBehandlingsresultat
    }

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)

        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = Beslutning.GODKJENT,
            behandlingErAutomatiskBesluttet = true,
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        return vedtakService.oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }

    fun omgjørBehandlingTilManuellOgKjørSteg(behandling: Behandling, steg: StegType): Behandling {
        val omgjortBehandling = behandlingService.omgjørTilManuellBehandling(behandling)

        return when (steg) {
            StegType.VILKÅRSVURDERING ->
                tilbakestillBehandlingTilBehandlingsresultatService
                    .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = omgjortBehandling.id)

            else -> throw Feil("Steg $steg er ikke støttet ved omgjøring av automatisk behandling til manuell.")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakService::class.java)
    }
}
