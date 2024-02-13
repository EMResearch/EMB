package no.nav.familie.ba.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonidentRepository : JpaRepository<Personident, String> {
    @Query("SELECT p FROM Personident p WHERE p.fødselsnummer = :fødselsnummer")
    fun findByFødselsnummerOrNull(fødselsnummer: String): Personident?
}
