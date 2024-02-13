package no.nav.familie.ba.sak.integrasjoner.økonomi

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DataChunkRepository : JpaRepository<DataChunk, Long> {

    @Query("SELECT dc FROM DataChunk dc WHERE dc.transaksjonsId = :transaksjonsId AND dc.chunkNr = :chunkNr")
    fun findByTransaksjonsIdAndChunkNr(transaksjonsId: UUID, chunkNr: Int): DataChunk?
    fun findByTransaksjonsId(transaksjonsId: UUID): List<DataChunk>
    fun findByErSendt(erSendt: Boolean): List<DataChunk>
}
