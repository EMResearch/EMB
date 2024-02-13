package no.nav.familie.ba.sak.kjerne.brev.mottaker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrevmottakerRepository : JpaRepository<Brevmottaker, Long> {
    @Query(value = "SELECT b FROM Brevmottaker b WHERE b.behandlingId = :behandlingId")
    fun finnBrevMottakereForBehandling(behandlingId: Long): List<Brevmottaker>
}
