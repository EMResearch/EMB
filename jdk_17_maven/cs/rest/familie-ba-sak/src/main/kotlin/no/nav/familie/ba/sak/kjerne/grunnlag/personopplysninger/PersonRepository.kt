package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {

    @Query(
        "SELECT p FROM Person p" +
            " WHERE p.aktør = :aktør",
    )
    fun findByAktør(aktør: Aktør): List<Person>

    @Query(
        "SELECT distinct b.fagsak FROM Person p" +
            " JOIN p.personopplysningGrunnlag pg" +
            " JOIN Behandling b ON b.id = pg.behandlingId" +
            " WHERE p.aktør = :aktør" +
            " AND pg.aktiv = true",
    )
    fun findFagsakerByAktør(aktør: Aktør): List<Fagsak>
}
