package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface JournalføringRepository : JpaRepository<DbJournalpost, Long> {

    @Query("SELECT j FROM Journalpost j JOIN j.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<DbJournalpost>
}
