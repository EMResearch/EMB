package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import org.springframework.stereotype.Service

@Service
class SkyggesakService(
    private val skyggesakRepository: SkyggesakRepository,
) {
    fun opprettSkyggesak(fagsak: Fagsak) {
        skyggesakRepository.save(Skyggesak(fagsakId = fagsak.id))
    }
}
