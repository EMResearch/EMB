package no.nav.familie.ba.sak.kjerne.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface ØkonomiSimuleringMottakerRepository : JpaRepository<ØkonomiSimuleringMottaker, Long> {

    @Query(value = "SELECT sm FROM OkonomiSimuleringMottaker sm JOIN sm.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<ØkonomiSimuleringMottaker>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM OkonomiSimuleringMottaker sm where sm.behandling.id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: Long)
}
