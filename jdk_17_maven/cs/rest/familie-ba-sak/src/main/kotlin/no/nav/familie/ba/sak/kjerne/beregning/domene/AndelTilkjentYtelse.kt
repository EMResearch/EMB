package no.nav.familie.ba.sak.kjerne.beregning.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.YtelsetypeBA
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseForVedtaksperioderTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledSegmenter
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AndelTilkjentYtelse")
@Table(name = "ANDEL_TILKJENT_YTELSE")
data class AndelTilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andel_tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "andel_tilkjent_ytelse_seq_generator",
        sequenceName = "andel_tilkjent_ytelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @ManyToOne(cascade = [CascadeType.MERGE])
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    var tilkjentYtelse: TilkjentYtelse,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @Column(name = "kalkulert_utbetalingsbelop", nullable = false)
    val kalkulertUtbetalingsbeløp: Int,

    @Column(name = "stonad_fom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadFom: YearMonth,

    @Column(name = "stonad_tom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadTom: YearMonth,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: YtelseType,

    @Column(name = "sats", nullable = false)
    val sats: Int,

    @Column(name = "prosent", nullable = false)
    val prosent: BigDecimal,

    // kildeBehandlingId, periodeOffset og forrigePeriodeOffset trengs kun i forbindelse med
    // iverksetting/konsistensavstemming, og settes først ved generering av selve oppdraget mot økonomi.

    // Samme informasjon finnes i utbetalingsoppdraget på hver enkelt sak, men for å gjøre operasjonene mer forståelig
    // og enklere å jobbe med har vi valgt å trekke det ut hit.

    @Column(name = "kilde_behandling_id")
    var kildeBehandlingId: Long? = null, // Brukes til å finne hvilke behandlinger som skal konsistensavstemmes

    @Column(name = "periode_offset")
    var periodeOffset: Long? = null, // Brukes for å koble seg på tidligere kjeder sendt til økonomi

    @Column(name = "forrige_periode_offset")
    var forrigePeriodeOffset: Long? = null,

    @Column(name = "nasjonalt_periodebelop")
    val nasjonaltPeriodebeløp: Int?,

    @Column(name = "differanseberegnet_periodebelop")
    val differanseberegnetPeriodebeløp: Int? = null,

) : BaseEntitet() {

    val periode
        get() = MånedPeriode(stønadFom, stønadTom)

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other as AndelTilkjentYtelse
        return Objects.equals(behandlingId, annen.behandlingId) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(stønadFom, annen.stønadFom) &&
            Objects.equals(stønadTom, annen.stønadTom) &&
            Objects.equals(aktør, annen.aktør) &&
            Objects.equals(nasjonaltPeriodebeløp, annen.nasjonaltPeriodebeløp) &&
            Objects.equals(differanseberegnetPeriodebeløp, annen.differanseberegnetPeriodebeløp)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            behandlingId,
            type,
            kalkulertUtbetalingsbeløp,
            stønadFom,
            stønadTom,
            aktør,
            nasjonaltPeriodebeløp,
            differanseberegnetPeriodebeløp,
        )
    }

    override fun toString(): String {
        return "AndelTilkjentYtelse(id = $id, behandling = $behandlingId, type = $type, prosent = $prosent," +
            "beløp = $kalkulertUtbetalingsbeløp, stønadFom = $stønadFom, stønadTom = $stønadTom, periodeOffset = $periodeOffset, " +
            "forrigePeriodeOffset = $forrigePeriodeOffset, kildeBehandlingId = $kildeBehandlingId, nasjonaltPeriodebeløp = $nasjonaltPeriodebeløp, differanseberegnetBeløp = $differanseberegnetPeriodebeløp)"
    }

    fun overlapperMed(andelFraAnnenBehandling: AndelTilkjentYtelse): Boolean {
        return this.type == andelFraAnnenBehandling.type &&
            this.overlapperPeriode(andelFraAnnenBehandling.periode)
    }

    fun overlapperPeriode(måndePeriode: MånedPeriode): Boolean =
        this.stønadFom <= måndePeriode.tom &&
            this.stønadTom >= måndePeriode.fom

    fun stønadsPeriode() = MånedPeriode(this.stønadFom, this.stønadTom)

    fun erUtvidet() = this.type == YtelseType.UTVIDET_BARNETRYGD

    fun erSmåbarnstillegg() = this.type == YtelseType.SMÅBARNSTILLEGG

    fun erSøkersAndel() = erUtvidet() || erSmåbarnstillegg()

    fun erLøpende(): Boolean = this.stønadTom > YearMonth.now()

    fun erDeltBosted() = this.prosent == BigDecimal(50)

    fun erEøs(personResultater: Set<PersonResultat>) = vurdertEtter(personResultater) == Regelverk.EØS_FORORDNINGEN

    fun vurdertEtter(personResultater: Set<PersonResultat>): Regelverk {
        val relevanteVilkårsResultaer = finnRelevanteVilkårsresulaterForRegelverk(personResultater)

        return if (relevanteVilkårsResultaer.isEmpty()) {
            Regelverk.NASJONALE_REGLER
        } else if (relevanteVilkårsResultaer.all { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }) {
            Regelverk.EØS_FORORDNINGEN
        } else if (relevanteVilkårsResultaer.all { it.vurderesEtter == Regelverk.NASJONALE_REGLER }) {
            Regelverk.NASJONALE_REGLER
        } else {
            Regelverk.NASJONALE_REGLER
        }
    }

    fun erAndelSomSkalSendesTilOppdrag(): Boolean {
        return this.kalkulertUtbetalingsbeløp != 0
    }

    fun erAndelSomharNullutbetaling() = this.kalkulertUtbetalingsbeløp == 0 &&
        this.differanseberegnetPeriodebeløp != null &&
        this.differanseberegnetPeriodebeløp <= 0

    private fun finnRelevanteVilkårsresulaterForRegelverk(
        personResultater: Set<PersonResultat>,
    ): List<VilkårResultat> =
        personResultater
            .filter { !it.erSøkersResultater() }
            .filter { this.aktør == it.aktør }
            .flatMap { it.vilkårResultater }
            .filter {
                this.stønadFom > (it.periodeFom ?: TIDENES_MORGEN).toYearMonth() &&
                    (it.periodeTom == null || this.stønadFom <= it.periodeTom?.toYearMonth())
            }
            .filter { vilkårResultat ->
                regelverkavhenigeVilkår().any { it == vilkårResultat.vilkårType }
            }
}

fun List<AndelTilkjentYtelse>.slåSammenBack2BackAndelsperioderMedSammeBeløp(): List<AndelTilkjentYtelse> {
    if (this.size <= 1) return this
    val sorterteAndeler = this.sortedBy { it.stønadFom }
    val sammenslåtteAndeler = mutableListOf<AndelTilkjentYtelse>()
    var andel = sorterteAndeler.firstOrNull()
    sorterteAndeler.forEach { andelTilkjentYtelse ->
        andel = andel ?: andelTilkjentYtelse
        val back2BackAndelsperiodeMedSammeBeløp = this.singleOrNull {
            andel!!.stønadTom.plusMonths(1).equals(it.stønadFom) &&
                andel!!.aktør == it.aktør &&
                andel!!.kalkulertUtbetalingsbeløp == it.kalkulertUtbetalingsbeløp &&
                andel!!.type == it.type
        }
        andel = if (back2BackAndelsperiodeMedSammeBeløp != null) {
            andel!!.copy(stønadTom = back2BackAndelsperiodeMedSammeBeløp.stønadTom)
        } else {
            sammenslåtteAndeler.add(andel!!)
            null
        }
    }
    if (andel != null) sammenslåtteAndeler.add(andel!!)
    return sammenslåtteAndeler
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagVertikaleSegmenter(): Map<LocalDateSegment<Int>, List<AndelTilkjentYtelseMedEndreteUtbetalinger>> {
    return this.utledSegmenter()
        .fold(mutableMapOf()) { acc, segment ->
            val andelerForSegment = this.filter {
                segment.localDateInterval.overlaps(
                    LocalDateInterval(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                    ),
                )
            }
            acc[segment] = andelerForSegment
            acc
        }
}

enum class YtelseType(val klassifisering: String) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
    ;

    fun erKnyttetTilSøker() = this == SMÅBARNSTILLEGG || this == UTVIDET_BARNETRYGD

    fun hentSatsTyper(): List<SatsType> = when (this) {
        ORDINÆR_BARNETRYGD -> listOf(SatsType.ORBA, SatsType.TILLEGG_ORBA)
        UTVIDET_BARNETRYGD -> listOf(SatsType.UTVIDET_BARNETRYGD)
        SMÅBARNSTILLEGG -> listOf(SatsType.SMA)
    }

    fun tilYtelseType(): YtelsetypeBA = when (this) {
        ORDINÆR_BARNETRYGD -> YtelsetypeBA.ORDINÆR_BARNETRYGD
        UTVIDET_BARNETRYGD -> YtelsetypeBA.UTVIDET_BARNETRYGD
        SMÅBARNSTILLEGG -> YtelsetypeBA.SMÅBARNSTILLEGG
    }

    fun tilSatsType(person: Person, ytelseDato: LocalDate) = when (this) {
        ORDINÆR_BARNETRYGD -> if (ytelseDato.toYearMonth() < person.hentSeksårsdag().toYearMonth()) {
            SatsType.TILLEGG_ORBA
        } else {
            SatsType.ORBA
        }

        UTVIDET_BARNETRYGD -> SatsType.UTVIDET_BARNETRYGD
        SMÅBARNSTILLEGG -> SatsType.SMA
    }
}

private fun regelverkavhenigeVilkår(): List<Vilkår> {
    return listOf(
        Vilkår.BOR_MED_SØKER,
        Vilkår.BOSATT_I_RIKET,
        Vilkår.LOVLIG_OPPHOLD,
    )
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.hentAndelerForSegment(
    vertikaltSegmentForVedtaksperiode: LocalDateSegment<Int>,
) = this.filter {
    vertikaltSegmentForVedtaksperiode.localDateInterval.overlaps(
        LocalDateInterval(
            it.stønadFom.førsteDagIInneværendeMåned(),
            it.stønadTom.sisteDagIInneværendeMåned(),
        ),
    )
}

fun List<AndelTilkjentYtelse>.tilTidslinjerPerPersonOgType(): Map<Pair<Aktør, YtelseType>, AndelTilkjentYtelseTidslinje> =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        AndelTilkjentYtelseTidslinje(
            andelerTilkjentYtelsePåPerson,
        )
    }

fun List<AndelTilkjentYtelse>.tilTidslinjerPerAktørOgType() =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        AndelTilkjentYtelseForVedtaksperioderTidslinje(
            andelerTilkjentYtelsePåPerson,
        )
    }
