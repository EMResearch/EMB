package no.nav.familie.ba.sak.kjerne.institusjon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InstitusjonRepository : JpaRepository<Institusjon, Long> {
    fun findByOrgNummer(orgNummer: String): Institusjon?
    fun findByTssEksternId(tssEksternId: String): Institusjon?
}
