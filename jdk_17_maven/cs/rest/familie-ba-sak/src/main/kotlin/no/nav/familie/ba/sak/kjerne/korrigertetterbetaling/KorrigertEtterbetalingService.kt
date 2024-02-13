package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class KorrigertEtterbetalingService(
    private val korrigertEtterbetalingRepository: KorrigertEtterbetalingRepository,
    private val loggService: LoggService,
) {

    fun finnAktivtKorrigeringPåBehandling(behandlingId: Long): KorrigertEtterbetaling? =
        korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandlingId)

    fun finnAlleKorrigeringerPåBehandling(behandlingId: Long): List<KorrigertEtterbetaling> =
        korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandlingId)

    @Transactional
    fun lagreKorrigertEtterbetaling(korrigertEtterbetaling: KorrigertEtterbetaling): KorrigertEtterbetaling {
        val behandling = korrigertEtterbetaling.behandling

        finnAktivtKorrigeringPåBehandling(behandling.id)?.let {
            it.aktiv = false
            korrigertEtterbetalingRepository.saveAndFlush(it)
        }

        loggService.opprettKorrigertEtterbetalingLogg(behandling, korrigertEtterbetaling)
        return korrigertEtterbetalingRepository.save(korrigertEtterbetaling)
    }

    @Transactional
    fun settKorrigeringPåBehandlingTilInaktiv(behandling: Behandling): KorrigertEtterbetaling? =
        finnAktivtKorrigeringPåBehandling(behandling.id)?.apply {
            aktiv = false
            loggService.opprettKorrigertEtterbetalingLogg(behandling, this)
        }
}
