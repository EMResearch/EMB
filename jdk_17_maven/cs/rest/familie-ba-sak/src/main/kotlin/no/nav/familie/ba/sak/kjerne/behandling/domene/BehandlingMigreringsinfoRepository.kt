package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface BehandlingMigreringsinfoRepository : JpaRepository<BehandlingMigreringsinfo, Long> {

    @Query(
        """SELECT MIN(bm.migreringsdato) FROM BehandlingMigreringsinfo bm 
            INNER JOIN Behandling b ON bm.behandling.id = b.id 
            INNER JOIN Fagsak f ON b.fagsak.id = f.id 
            WHERE f.id=:fagsakId""",
    )
    fun finnSisteMigreringsdatoPåFagsak(fagsakId: Long): LocalDate?

    @Query("SELECT bm FROM BehandlingMigreringsinfo bm where bm.behandling.id=:behandlingId ")
    fun findByBehandlingId(behandlingId: Long): BehandlingMigreringsinfo?

    @Query(
        """SELECT DISTINCT(f.id) FROM BehandlingMigreringsinfo bm 
            INNER JOIN Behandling b ON bm.behandling.id = b.id 
            INNER JOIN Fagsak f ON b.fagsak.id = f.id
            WHERE bm.migreringsdato >= :migreringsdato
            AND b.opprettetÅrsak = 'MIGRERING'
    """,
    )
    fun finnMuligeMigreringerMedManglendeSats(migreringsdato: LocalDate): List<Long>
}
