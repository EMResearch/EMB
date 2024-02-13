package no.nav.familie.ba.sak.kjerne.totrinnskontroll

import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TotrinnskontrollRepository : JpaRepository<Totrinnskontroll, Long> {
    @Query("SELECT t FROM Totrinnskontroll t JOIN t.behandling b WHERE b.id = :behandlingId AND t.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Totrinnskontroll?
}
