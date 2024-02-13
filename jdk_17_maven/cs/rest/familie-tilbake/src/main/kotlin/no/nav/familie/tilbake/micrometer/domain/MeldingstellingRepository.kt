package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import java.time.LocalDate
import java.util.UUID

interface MeldingstellingRepository :
    RepositoryInterface<Meldingstelling, UUID>,
    InsertUpdateRepository<Meldingstelling> {

    fun findByFagsystemAndTypeAndStatusAndDato(
        fagsystem: Fagsystem,
        type: Meldingstype,
        status: Mottaksstatus,
        dato: LocalDate = LocalDate.now(),
    ): Meldingstelling?

    fun findByType(type: Meldingstype): List<Meldingstelling>

    @Query(
        """SELECT fagsystem, dato, SUM(antall) as antall FROM meldingstelling
              WHERE type = :type
              GROUP BY fagsystem, dato""",
    )
    fun summerAntallForType(type: Meldingstype): List<ForekomsterPerDag>

    @Modifying
    @Query(
        """UPDATE meldingstelling SET antall = antall + 1 
              WHERE fagsystem = :fagsystem
              AND type = :type
              AND status = :status
              AND dato = :dato""",
    )
    fun oppdaterTeller(
        fagsystem: Fagsystem,
        type: Meldingstype,
        status: Mottaksstatus,
        dato: LocalDate = LocalDate.now(),
    )

    // language=PostgreSQL
    @Query(
        """SELECT fagsystem, 
                     extract(ISOYEAR from behandling.opprettet_dato) as år,  
                     extract(WEEK from behandling.opprettet_dato) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status <> 'AVSLUTTET'
              GROUP BY fagsystem, år, uke""",
    )
    fun finnÅpneBehandlinger(): List<ForekomsterPerUke>

    // language=PostgreSQL
    @Query(
        """SELECT fagsystem, behandlingssteg, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsstegstilstand b ON behandling.id = b.behandling_id
              WHERE status <> 'AVSLUTTET'
              AND behandlingsstegsstatus = 'KLAR'
              GROUP BY fagsystem, behandlingssteg""",
    )
    fun finnKlarTilBehandling(): List<BehandlingerPerSteg>

    // language=PostgreSQL
    @Query(
        """SELECT fagsystem, behandlingssteg, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsstegstilstand b ON behandling.id = b.behandling_id
              WHERE status <> 'AVSLUTTET'
              AND behandlingsstegsstatus = 'VENTER'
              GROUP BY fagsystem, behandlingssteg""",
    )
    fun finnVentendeBehandlinger(): List<BehandlingerPerSteg>

    // language=PostgreSQL
    @Query(
        """SELECT fagsystem, 
                     b.brevtype, 
                     extract(ISOYEAR from b.opprettet_tid) as år,  
                     extract(WEEK from b.opprettet_tid) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              GROUP BY fagsystem, b.brevtype, år, uke""",
    )
    fun finnSendteBrev(): List<BrevPerUke>

    // language=PostgreSQL
    @Query(
        """SELECT fagsystem, 
                     behandlingsresultat.type as vedtakstype, 
                     extract(ISOYEAR from avsluttet_dato) as år,
                     extract(WEEK from avsluttet_dato) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              WHERE status = 'AVSLUTTET'
              GROUP BY fagsystem, vedtakstype, år, uke""",
    )
    fun finnVedtak(): List<VedtakPerUke>
}
