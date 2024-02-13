package no.nav.familie.ba.sak.kjerne.behandling.domene

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandlingNullable(behandlingId: Long): Behandling?

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query(value = "SELECT b FROM Behandling b WHERE b.fagsak.id = :fagsakId AND status = :status")
    fun finnBehandlinger(fagsakId: Long, status: BehandlingStatus): List<Behandling>

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id in :fagsakIder AND f.arkivert = false")
    fun finnBehandlinger(fagsakIder: Set<Long>): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true AND f.arkivert = false")
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true AND b.status <> 'AVSLUTTET' AND f.arkivert = false")
    fun findByFagsakAndAktivAndOpen(fagsakId: Long): Behandling?

    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                        SELECT DISTINCT ON (b.fk_fagsak_id) b.id
                        FROM behandling b
                                 INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                 INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                        WHERE f.status = 'LØPENDE'
                          AND ty.utbetalingsoppdrag IS NOT NULL
                          AND f.arkivert = false
                        ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC)
                        
                        select sum(aty.kalkulert_utbetalingsbelop) 
                        from andel_tilkjent_ytelse aty
                        where aty.stonad_fom <= :måned
                          AND aty.stonad_tom >= :måned
                        AND aty.fk_behandling_id in (SELECT silp.id FROM sisteiverksattebehandlingfraløpendefagsak silp)""",
        nativeQuery = true,
    )
    fun hentTotalUtbetalingForMåned(måned: LocalDateTime): Long

    /* Denne henter først siste iverksatte behandling på en løpende fagsak.
     * Finner så alle perioder på siste iverksatte behandling
     * Finner deretter første behandling en periode oppstod i, som er det som skal avstemmes
     */
    @Query(
        value = """SELECT DISTINCT ON (b.fk_fagsak_id) b.id
                    FROM behandling b
                           INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                           INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                    WHERE f.status = 'LØPENDE'
                      AND ty.utbetalingsoppdrag IS NOT NULL
                      AND f.arkivert = false
                    ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC""",
        nativeQuery = true,
    )
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(): List<Long>

    @Query(
        """select b from Behandling b
                           inner join TilkjentYtelse ty on b.id = ty.behandling.id
                        where b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL""",
    )
    fun finnIverksatteBehandlinger(fagsakId: Long): List<Behandling>

    @Query(
        """SELECT DISTINCT ON(b.fk_fagsak_id) b.*
            FROM behandling b
                     INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                     INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
            WHERE f.id = :fagsakId
              AND ty.utbetalingsoppdrag IS NOT NULL
              AND f.arkivert = false
              AND b.status = 'AVSLUTTET'
            ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC""",
        nativeQuery = true,
    )
    fun finnSisteIverksatteBehandling(fagsakId: Long): Behandling?

    @Query(
        """
            select b from Behandling b
                            where b.fagsak.id = :fagsakId and b.status = 'IVERKSETTER_VEDTAK'
        """,
    )
    fun finnBehandlingerSomHolderPåÅIverksettes(fagsakId: Long): List<Behandling>

    /**
     *  Finner behandlinger som ligger til godkjenning.
     *  Dvs. behandlingen er på 'beslutte vedtak'-steget ('beslutte vedtak' er det siste steget på behandlingen) og dette steget er ikke utført enda
     */
    @Query(
        """select b from Behandling b
                inner join BehandlingStegTilstand bst on b.id = bst.behandling.id
                where b.fagsak.id = :fagsakId AND bst.behandlingSteg = 'BESLUTTE_VEDTAK' AND bst.behandlingStegStatus = 'IKKE_UTFØRT' 
                    AND bst.id = (
                        select bst2.id
                        from BehandlingStegTilstand bst2 
                        where bst2.behandling.id = b.id 
                        ORDER BY bst2.opprettetTidspunkt DESC LIMIT 1
                    )""",
    )
    fun finnBehandlingerSomLiggerTilGodkjenning(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    @Lock(LockModeType.NONE)
    @Query("SELECT count(*) FROM Behandling b JOIN b.fagsak f WHERE NOT b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnAntallBehandlingerIkkeAvsluttet(): Long

    @Lock(LockModeType.NONE)
    @Query("SELECT b.opprettetTidspunkt FROM Behandling b JOIN b.fagsak f WHERE NOT b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnOpprettelsestidspunktPåÅpneBehandlinger(): List<LocalDateTime>

    @Lock(LockModeType.NONE)
    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE b.opprettetTidspunkt < :opprettetFør AND b.status <> 'AVSLUTTET' AND f.arkivert = false")
    fun finnÅpneBehandlinger(opprettetFør: LocalDateTime): List<Behandling>

    @Lock(LockModeType.NONE)
    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE b.status <> 'AVSLUTTET' AND b.underkategori = 'UTVIDET' AND f.arkivert = false")
    fun finnÅpneUtvidetBarnetrygdBehandlinger(): List<Behandling>

    @Query("SELECT new kotlin.Pair(b.opprettetÅrsak, count(*)) from Behandling b group by b.opprettetÅrsak")
    fun finnAntallBehandlingerPerÅrsak(): List<Pair<BehandlingÅrsak, Long>>

    @Query("SELECT b.id from Behandling b where b.opprettetÅrsak in (:opprettetÅrsak)")
    fun finnBehandlingIdMedOpprettetÅrsak(opprettetÅrsak: List<BehandlingÅrsak>): List<Long>

    @Query(
        "SELECT new kotlin.Pair(b.id, p.fødselsnummer) from Behandling b " +
            "INNER JOIN Fagsak f ON f.id = b.fagsak.id INNER JOIN Aktør a on f.aktør.aktørId = a.aktørId " +
            "INNER JOIN Personident p on p.aktør.aktørId = a.aktørId " +
            "where b.id in (:behandlingIder) AND p.aktiv=true AND f.status = 'LØPENDE' ",
    )
    fun finnAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): List<Pair<Long, String>>

    @Query(
        "SELECT new kotlin.Pair(b.id, i.tssEksternId) from Behandling b " +
            "INNER JOIN Fagsak f ON f.id = b.fagsak.id " +
            "INNER JOIN Institusjon i on i.id = f.institusjon.id " +
            "where b.id in (:behandlingIder) AND f.institusjon IS NOT NULL AND f.status = 'LØPENDE' ",
    )
    fun finnTssEksternIdForBehandlinger(behandlingIder: List<Long>): List<Pair<Long, String>>

    @Query(value = "SELECT b.status FROM Behandling b WHERE b.id = :behandlingId")
    fun finnStatus(behandlingId: Long): BehandlingStatus

    @Query(
        """select distinct(b.id) from behandling b
            join fagsak f on f.id = b.fk_fagsak_id
            join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
        where b.aktiv = true
        AND f.status = 'LØPENDE'
        AND b.status = 'AVSLUTTET'
        AND ty.stonad_tom is null
        AND ty.utbetalingsoppdrag is null
        LIMIT :limit""",
        nativeQuery = true,
    )
    fun finnAktiveBehandlingerSomManglerStønadTom(limit: Int): List<Long>
}
