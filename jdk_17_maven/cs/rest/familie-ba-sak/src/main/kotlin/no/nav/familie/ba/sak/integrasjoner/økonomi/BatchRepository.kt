package no.nav.familie.ba.sak.integrasjoner.økonomi

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface BatchRepository : JpaRepository<Batch, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(batch: Batch): Batch

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    override fun findById(id: Long): Optional<Batch>

    @Query("SELECT k FROM Batch k where kjøreDato = :dato AND status = 'LEDIG'")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByKjøredatoAndLedig(dato: LocalDate): Batch?
}
