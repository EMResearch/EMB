package no.nav.familie.ba.sak.kjerne.verge

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VergeService(
    val vergeRepository: VergeRepository,
) {

    @Transactional
    fun oppdaterVergeForBehandling(behandling: Behandling, verge: Verge) {
        val vergeRegistrertFraFør = vergeRepository.findByBehandling(behandling)
        if (vergeRegistrertFraFør != null) {
            vergeRepository.delete(vergeRegistrertFraFør)
            vergeRepository.flush()
        }
        vergeRepository.save(verge)
    }
}
