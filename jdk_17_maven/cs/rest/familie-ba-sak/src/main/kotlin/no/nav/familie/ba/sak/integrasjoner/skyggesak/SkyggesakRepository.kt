package no.nav.familie.ba.sak.integrasjoner.skyggesak

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SkyggesakRepository : JpaRepository<Skyggesak, Long> {

    @Query(value = "SELECT s FROM Skyggesak s WHERE s.sendtTidspunkt IS NULL")
    fun finnSkyggesakerKlareForSending(page: Pageable): List<Skyggesak>

    @Query(value = "SELECT s FROM Skyggesak s WHERE s.sendtTidspunkt IS NOT NULL")
    fun finnSkyggesakerSomErSendt(): List<Skyggesak>
}
