package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingRepository: BehandlingRepository,
    private val påVentService: SettPåVentService,
    private val loggService: LoggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingTilPåMaskinellVent(behandlingId: Long, årsak: SettPåMaskinellVentÅrsak) {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        if (!behandling.aktiv) {
            error("Behandling=$behandlingId er ikke aktiv")
        }
        val behandlingStatus = behandling.status
        if (behandlingStatus !== BehandlingStatus.UTREDES && behandlingStatus !== BehandlingStatus.SATT_PÅ_VENT) {
            error("Behandling=$behandlingId kan ikke settes på maskinell vent då status=$behandlingStatus")
        }
        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingRepository.saveAndFlush(behandling)
        loggService.opprettSettPåMaskinellVent(behandling, årsak.årsak)
    }

    /**
     * @param behandlingSomFerdigstilles er behandlingen som ferdigstilles i [no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling]
     *  Den er mest brukt for å logge hvilken behandling det er som ferdigstilles og hvilken som blir deaktivert
     *
     * @return boolean som tilsier om en behandling er reaktivert eller ikke
     */
    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(behandlingSomFerdigstilles: Behandling): Boolean {
        val fagsakId = behandlingSomFerdigstilles.fagsak.id

        val behandlingPåVent = finnBehandlingPåMaskinellVent(fagsakId) ?: return false
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId)

        validerBehandlinger(aktivBehandling, behandlingPåVent)

        aktiverBehandlingPåVent(aktivBehandling, behandlingPåVent, behandlingSomFerdigstilles)
        tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåVent)
        loggService.opprettTattAvMaskinellVent(behandlingPåVent)
        return true
    }

    private fun finnBehandlingPåMaskinellVent(
        fagsakId: Long,
    ): Behandling? =
        behandlingRepository.finnBehandlinger(fagsakId, BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            .takeIf { it.isNotEmpty() }
            ?.let { it.singleOrNull() ?: error("Forventer kun en behandling på vent for fagsak=$fagsakId") }

    private fun aktiverBehandlingPåVent(
        aktivBehandling: Behandling?,
        behandlingPåVent: Behandling,
        behandlingSomFerdigstilles: Behandling,
    ) {
        logger.info(
            "Deaktiverer aktivBehandling=${aktivBehandling?.id}" +
                " aktiverer behandlingPåVent=${behandlingPåVent.id}" +
                " behandlingSomFerdigstilles=${behandlingSomFerdigstilles.id}",
        )

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingRepository.saveAndFlush(aktivBehandling)
        }

        behandlingPåVent.aktiv = true
        behandlingPåVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåVent.status = utledStatusForBehandlingPåVent(behandlingPåVent)

        behandlingRepository.saveAndFlush(behandlingPåVent)
    }

    private fun validerBehandlinger(aktivBehandling: Behandling?, behandlingPåVent: Behandling) {
        if (behandlingPåVent.aktiv) {
            error("Åpen behandling har feil tilstand $behandlingPåVent")
        }
        if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw BehandlingErIkkeAvsluttetException(aktivBehandling)
        }
    }

    /**
     * Hvis behandlingen er satt på vent av saksbehandler så skal statusen settes tilbake til SATT_PÅ_VENT
     * Ellers settes UTREDES
     */
    private fun utledStatusForBehandlingPåVent(behandlingPåVent: Behandling) =
        påVentService.finnAktivSettPåVentPåBehandling(behandlingPåVent.id)
            ?.let { BehandlingStatus.SATT_PÅ_VENT }
            ?: BehandlingStatus.UTREDES
}

enum class SettPåMaskinellVentÅrsak(val årsak: String) {
    SATSENDRING("Satsendring"),
}

class BehandlingErIkkeAvsluttetException(val behandling: Behandling) :
    RuntimeException("Behandling=${behandling.id} har status=${behandling.status} og er ikke avsluttet")
