package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.annotation.Timed
import jakarta.persistence.LockModeType
import no.nav.familie.ba.sak.ekstern.skatteetaten.UtvidetSkatt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Optional

@Repository
interface FagsakRepository : JpaRepository<Fagsak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Lock(LockModeType.NONE)
    override fun findById(id: Long): Optional<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnFagsak(fagsakId: Long): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.type = :type and f.arkivert = false")
    fun finnFagsakForAktør(aktør: Aktør, type: FagsakType = FagsakType.NORMAL): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.type = 'INSTITUSJON' and f.status <> 'AVSLUTTET' and f.arkivert = false and f.institusjon.orgNummer = :orgNummer")
    fun finnFagsakForInstitusjonOgOrgnummer(aktør: Aktør, orgNummer: String): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakerForAktør(aktør: Aktør): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f.id from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(page: Pageable): Slice<Long>

    @Query(
        value = """SELECT f.id
            FROM   Fagsak f
            WHERE  NOT EXISTS (
                    SELECT 1
                    FROM   satskjoering
                    WHERE  fk_fagsak_id = f.id
                    AND sats_tid  = :satsTidspunkt
                ) AND f.status = 'LØPENDE' AND f.arkivert = false""",
        nativeQuery = true,
    )
    fun finnLøpendeFagsakerForSatsendring(satsTidspunkt: LocalDate, page: Pageable): Page<Long>

    @Query(
        value = """WITH sisteiverksatte AS (
                    SELECT DISTINCT ON (b.fk_fagsak_id) b.id, b.fk_fagsak_id, stonad_tom
                    FROM behandling b
                             INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                             INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                    WHERE f.status = 'LØPENDE'
                      AND f.arkivert = FALSE
                    ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC)
                
                SELECT silp.fk_fagsak_id
                FROM sisteiverksatte silp
                WHERE  silp.stonad_tom < DATE_TRUNC('month', NOW())""",
        nativeQuery = true,
    )
    fun finnFagsakerSomSkalAvsluttes(): List<Long>

    /**
     * Denne skal plukke fagsaker som løper _og_ har barn født innenfor anngitt tidsintervall.
     * Brukes til å sende ut automatiske brev ved reduksjon 6 og 18 år blant annet.
     * Ved 18 år og dersom hele fagsaken opphører så skal det ikke sendes ut brev og derfor sjekker
     * vi kun løpende fagsaker.
     */
    @Query(
        value = """
        SELECT f FROM Fagsak f
        WHERE f.arkivert = false AND f.status = 'LØPENDE' AND f IN ( 
            SELECT b.fagsak FROM Behandling b 
            WHERE b.aktiv=true AND b.id IN (
                SELECT pg.behandlingId FROM PersonopplysningGrunnlag pg
                WHERE pg.aktiv=true AND pg.id IN (
                    SELECT p.personopplysningGrunnlag FROM Person p 
                    WHERE p.fødselsdato BETWEEN :fom AND :tom 
                    AND p.type = 'BARN'
                )
            )
        )
        """,
    )
    fun finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(fom: LocalDate, tom: LocalDate): Set<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak where arkivert = false")
    fun finnAntallFagsakerTotalt(): Long

    @Query(value = "SELECT f from Fagsak f where f.arkivert = false")
    fun hentFagsakerSomIkkeErArkivert(): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak f where f.status='LØPENDE' and f.arkivert = false")
    fun finnAntallFagsakerLøpende(): Long

    @Query(
        value = """
        SELECT p.foedselsnummer AS fnr,
               MAX(ty.endret_dato) AS sistevedtaksdato
        FROM andel_tilkjent_ytelse aty
                 INNER JOIN
             tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
                 INNER JOIN personident p ON aty.fk_aktoer_id = p.fk_aktoer_id
        WHERE ty.utbetalingsoppdrag IS NOT NULL
          AND aty.type = 'UTVIDET_BARNETRYGD'
          AND aty.stonad_fom <= :tom
          AND aty.stonad_tom >= :fom
          AND p.aktiv = TRUE
        GROUP BY p.foedselsnummer
    """,
        nativeQuery = true,
    )
    @Timed
    fun finnFagsakerMedUtvidetBarnetrygdInnenfor(fom: LocalDateTime, tom: LocalDateTime): List<UtvidetSkatt>

    @Query(
        """
            SELECT DISTINCT b.fagsak.id
            FROM AndelTilkjentYtelse aty
                JOIN Behandling b ON b.id = aty.behandlingId
                JOIN TilkjentYtelse ty ON b.id = ty.behandling.id
            WHERE
                    b.id in :iverksatteLøpendeBehandlinger
                AND NOT EXISTS (SELECT b2 from Behandling b2 where b2.fagsak.id = b.fagsak.id AND b2.status <> 'AVSLUTTET')
                AND NOT EXISTS (SELECT aty2 from AndelTilkjentYtelse aty2 where aty2.behandlingId = b.id AND aty2.type = 'SMÅBARNSTILLEGG' AND aty.stønadFom = :innværendeMåned)
                AND aty.type = 'SMÅBARNSTILLEGG'
                AND aty.stønadTom = :stønadTom
        """,
    )
    fun finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadTom: YearMonth = YearMonth.now().minusMonths(1),
        innværendeMåned: YearMonth = YearMonth.now(),
    ): List<Long>

    @Query(
        """
            SELECT DISTINCT b.fagsak.id
            FROM AndelTilkjentYtelse aty
                JOIN Behandling b ON b.id = aty.behandlingId
                JOIN TilkjentYtelse ty ON b.id = ty.behandling.id
            WHERE
                    b.id in :iverksatteLøpendeBehandlinger
                AND NOT EXISTS (SELECT b2 from Behandling b2 where b2.fagsak.id = b.fagsak.id AND b2.status <> 'AVSLUTTET')
                AND aty.type = 'SMÅBARNSTILLEGG'
                AND aty.stønadFom = :stønadFom
        """,
    )
    fun finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadFom: YearMonth = YearMonth.now(),
    ): List<Long>

    @Query(
        """
        SELECT distinct f from Fagsak f
         JOIN Behandling b ON b.fagsak.id = f.id
         JOIN AndelTilkjentYtelse aty ON aty.behandlingId = b.id
        WHERE aty.aktør = :aktør
        """,
    )
    fun finnFagsakerSomHarAndelerForAktør(aktør: Aktør): List<Fagsak>

    @Query(
        """
        SELECT distinct f FROM Fagsak f
            JOIN Behandling b ON f.id = b.fagsak.id
            WHERE f.status = 'LØPENDE' AND b.opprettetÅrsak in ('HELMANUELL_MIGRERING', 'MIGRERING') AND b.resultat NOT IN ('HENLAGT_FEILAKTIG_OPPRETTET', 'HENLAGT_SØKNAD_TRUKKET', 'HENLAGT_AUTOMATISK_FØDSELSHENDELSE', 'HENLAGT_TEKNISK_VEDLIKEHOLD')
        GROUP BY f.id
        HAVING COUNT(*) >= 2
        """,
    )
    fun finnFagsakerMedFlereMigreringsbehandlinger(): List<Fagsak>
}
