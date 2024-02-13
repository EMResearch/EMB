package no.nav.familie.ba.sak.statistikk.saksstatistikk.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SaksstatistikkMellomlagringRepository : JpaRepository<SaksstatistikkMellomlagring, Long> {

    @Query(value = "SELECT s FROM SaksstatistikkMellomlagring s WHERE s.sendtTidspunkt IS NULL")
    fun finnMeldingerKlarForSending(): List<SaksstatistikkMellomlagring>

    fun findByTypeAndTypeId(type: SaksstatistikkMellomlagringType, typeId: Long): List<SaksstatistikkMellomlagring>
}
