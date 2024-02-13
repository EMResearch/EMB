package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VilkårsvurderingRepository : JpaRepository<Vilkårsvurdering, Long> {

    @Query("SELECT v FROM Vilkårsvurdering v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vilkårsvurdering?
}
