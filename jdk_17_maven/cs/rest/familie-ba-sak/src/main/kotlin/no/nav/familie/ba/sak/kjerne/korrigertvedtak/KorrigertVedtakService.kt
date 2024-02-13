package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KorrigertVedtakService(
    private val korrigertVedtakRepository: KorrigertVedtakRepository,
    private val loggService: LoggService,
) {

    fun finnAktivtKorrigertVedtakPåBehandling(behandlingId: Long): KorrigertVedtak? =
        korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandlingId)

    @Transactional
    fun lagreKorrigertVedtak(korrigertVedtak: KorrigertVedtak): KorrigertVedtak {
        val behandling = korrigertVedtak.behandling

        finnAktivtKorrigertVedtakPåBehandling(behandling.id)?.let {
            it.aktiv = false
            korrigertVedtakRepository.saveAndFlush(it)
        }

        loggService.opprettKorrigertVedtakLogg(behandling, korrigertVedtak)
        return korrigertVedtakRepository.save(korrigertVedtak)
    }

    @Transactional
    fun settKorrigertVedtakPåBehandlingTilInaktiv(behandling: Behandling): KorrigertVedtak? =
        finnAktivtKorrigertVedtakPåBehandling(behandling.id)?.apply {
            aktiv = false
            loggService.opprettKorrigertVedtakLogg(behandling, this)
        }
}
