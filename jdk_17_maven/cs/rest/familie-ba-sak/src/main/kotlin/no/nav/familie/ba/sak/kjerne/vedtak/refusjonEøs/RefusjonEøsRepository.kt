package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RefusjonEøsRepository : JpaRepository<RefusjonEøs, Long> {
    @Query(value = "SELECT t FROM RefusjonEos t WHERE t.behandlingId = :behandlingId ORDER BY t.fom ASC")
    fun finnRefusjonEøsForBehandling(behandlingId: Long): List<RefusjonEøs>

    @Query(value = "SELECT f FROM RefusjonEos f WHERE f.id= :id")
    fun finnRefusjonEøs(id: Long): RefusjonEøs?
}
