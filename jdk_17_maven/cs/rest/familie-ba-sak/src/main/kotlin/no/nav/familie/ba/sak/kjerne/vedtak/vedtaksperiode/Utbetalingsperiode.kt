package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerSisteDagIPerioden
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dataklasser som brukes til frontend og backend når man jobber med vertikale utbetalingsperioder
 */

data class Utbetalingsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
    val ytelseTyper: List<YtelseType>,
    val antallBarn: Int,
    val utbetaltPerMnd: Int,
) : Vedtaksperiode

data class UtbetalingsperiodeDetalj(
    val person: RestPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val endringsårsak: Årsak?,
    val prosent: BigDecimal,
) {
    constructor(
        andel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) : this(
        person = personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }?.tilRestPerson()
            ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel"),
        ytelseType = andel.type,
        utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
        erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
        endringsårsak = andel.endreteUtbetalinger.singleOrNull()?.årsak,
        prosent = andel.prosent,
    )
}

fun List<UtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }

fun hentUtbetalingsperiodeForVedtaksperiode(
    utbetalingsperioder: List<Utbetalingsperiode>,
    fom: LocalDate?,
): Utbetalingsperiode {
    if (utbetalingsperioder.isEmpty()) {
        throw Feil("Det finnes ingen utbetalingsperioder ved utledning av utbetalingsperiode.")
    }
    val fomDato = fom?.toYearMonth() ?: inneværendeMåned()

    val sorterteUtbetalingsperioder = utbetalingsperioder.sortedBy { it.periodeFom }

    return sorterteUtbetalingsperioder.lastOrNull { it.periodeFom.toYearMonth() <= fomDato }
        ?: sorterteUtbetalingsperioder.firstOrNull()
        ?: throw Feil("Finner ikke gjeldende utbetalingsperiode ved fortsatt innvilget")
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<Utbetalingsperiode> {
    val andelerTidslinjePerAktørOgType = this.tilKombinertTidslinjePerAktørOgType()

    val utbetalingsPerioder = andelerTidslinjePerAktørOgType.perioder()
        .filter { !it.innhold.isNullOrEmpty() }
        .map { periode ->
            Utbetalingsperiode(
                periodeFom = periode.fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
                periodeTom = periode.tilOgMed.tilDagEllerSisteDagIPerioden().tilLocalDate(),
                ytelseTyper = periode.innhold!!.map { andelTilkjentYtelse -> andelTilkjentYtelse.type },
                utbetaltPerMnd = periode.innhold.sumOf { andelTilkjentYtelse -> andelTilkjentYtelse.kalkulertUtbetalingsbeløp },
                antallBarn = periode.innhold
                    .map { it.aktør }.toSet()
                    .count { aktør -> personopplysningGrunnlag.barna.any { barn -> barn.aktør == aktør } },
                utbetalingsperiodeDetaljer = periode.innhold.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag),
            )
        }

    return utbetalingsPerioder
}

internal fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.utledSegmenter(): List<LocalDateSegment<Int>> {
    // Dersom listen er tom så returnerer vi tom liste fordi at reduceren i
    // beregnUtbetalingsperioderUtenKlassifisering ikke takler tomme lister
    if (this.isEmpty()) return emptyList()

    val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(this.toSet())
    return utbetalingsPerioder.toSegments()
        .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
}

fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            person = personForAndel.tilRestPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent,
            endringsårsak = andel.endreteUtbetalinger.singleOrNull()?.årsak,
        )
    }
