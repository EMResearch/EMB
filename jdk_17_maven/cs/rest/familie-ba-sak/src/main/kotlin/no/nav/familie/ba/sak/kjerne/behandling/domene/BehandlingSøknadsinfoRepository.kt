package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BehandlingSøknadsinfoRepository : JpaRepository<BehandlingSøknadsinfo, Long> {

    @Query("SELECT bs FROM BehandlingSøknadsinfo bs where bs.behandling.id=:behandlingId ")
    fun findByBehandlingId(behandlingId: Long): Set<BehandlingSøknadsinfo>

    @Query(
        """
        SELECT count(distinct bs.journalpostId) AS antall,
               bs.brevkode AS brevkode,
               bs.erDigital AS erDigital
        FROM BehandlingSøknadsinfo bs
        WHERE bs.journalpostId IS NOT NULL
        AND bs.mottattDato >= :fomDato
        AND bs.mottattDato <= :tomDato
        GROUP BY bs.brevkode, bs.erDigital
    """,
    )
    fun hentAntallSøknaderIPeriode(fomDato: LocalDateTime, tomDato: LocalDateTime): List<AntallSøknaderPerGruppe>
}

interface AntallSøknaderPerGruppe {
    val antall: Int
    val brevkode: String
    val erDigital: Boolean
}
