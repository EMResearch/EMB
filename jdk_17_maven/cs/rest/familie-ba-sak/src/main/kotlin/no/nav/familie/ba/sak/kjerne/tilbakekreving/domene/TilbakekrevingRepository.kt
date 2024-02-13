package no.nav.familie.ba.sak.kjerne.tilbakekreving.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilbakekrevingRepository : JpaRepository<Tilbakekreving, Long> {

    @Query(value = "SELECT t FROM Tilbakekreving t JOIN t.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): Tilbakekreving?

    @Modifying
    @Query(value = "DELETE FROM Tilbakekreving t WHERE t.behandling.id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: Long)
}
