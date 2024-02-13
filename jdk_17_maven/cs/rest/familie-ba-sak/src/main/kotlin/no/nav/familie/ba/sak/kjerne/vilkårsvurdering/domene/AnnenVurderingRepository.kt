package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnnenVurderingRepository : JpaRepository<AnnenVurdering, Long> {

    @Query(value = "SELECT b FROM AnnenVurdering b WHERE b.personResultat = :personResultat AND b.type = :type")
    fun findBy(personResultat: PersonResultat, type: AnnenVurderingType): AnnenVurdering?
}
