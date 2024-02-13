package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FerdigstillBehandling(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val beregningService: BeregningService,
    private val behandlingService: BehandlingService,
    private val behandlingMetrikker: BehandlingMetrikker,
    private val loggService: LoggService,
    private val snikeIKøenService: SnikeIKøenService,
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        logger.info("Forsøker å ferdigstille behandling ${behandling.id}")

        val erHenlagt = behandlingHentOgPersisterService.hent(behandling.id).erHenlagt()

        if (behandling.status !== BehandlingStatus.IVERKSETTER_VEDTAK && !erHenlagt) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        if (!erHenlagt) {
            loggService.opprettFerdigstillBehandling(behandling)
        }

        behandlingMetrikker.oppdaterBehandlingMetrikker(behandling)
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK && behandling.resultat != Behandlingsresultat.AVSLÅTT) {
            oppdaterFagsakStatus(behandling = behandling)
        } else { // Dette betyr henleggelse.
            if (behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id).size == 1) {
                fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
            }
            behandlingHentOgPersisterService.finnAktivForFagsak(behandling.fagsak.id)?.aktiv = false
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)?.apply {
                aktiv = true
                behandlingHentOgPersisterService.lagreEllerOppdater(this)
            }
        }

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)
        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomFerdigstilles = behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun oppdaterFagsakStatus(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

        if (skalOppdatereStønadFomOgTomForIverksatteBehandlingerIkkeSendtTilOppdrag(tilkjentYtelse)) { // 0-utbetalinger/omregning
            tilkjentYtelse.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
            tilkjentYtelse.stønadFom = tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
        }

        val erLøpende = tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadTom >= inneværendeMåned() }
        if (erLøpende) {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    private fun skalOppdatereStønadFomOgTomForIverksatteBehandlingerIkkeSendtTilOppdrag(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelse.stønadFom == null && tilkjentYtelse.stønadTom == null && tilkjentYtelse.utbetalingsoppdrag == null

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FerdigstillBehandling::class.java)
    }
}
