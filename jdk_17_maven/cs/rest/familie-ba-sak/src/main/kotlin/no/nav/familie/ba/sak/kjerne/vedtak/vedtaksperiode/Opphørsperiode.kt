package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.Innhold
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidslinjeFraTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerSisteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import java.time.LocalDate

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
) : Vedtaksperiode

fun mapTilOpphørsperioder(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) {
        forrigeAndelerTilkjentYtelse.mapTilUtbetalingsperioder(forrigePersonopplysningGrunnlag)
    } else {
        emptyList()
    }
    val utbetalingsperioder =
        andelerTilkjentYtelse.mapTilUtbetalingsperioder(personopplysningGrunnlag)

    return listOf(
        finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(
            forrigeUtbetalingsperioder = forrigeUtbetalingsperioder,
            utbetalingsperioder = utbetalingsperioder,
        ),
        finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
        finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder),
    ).flatten().sortedBy { it.periodeFom }
}

private fun finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(
    forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
    utbetalingsperioder: List<Utbetalingsperiode>,
): List<Opphørsperiode> {
    val erUtbetalingOpphørtTidslinje = forrigeUtbetalingsperioder
        .tilTidslinje()
        .kombinerMed(utbetalingsperioder.tilTidslinje()) { forrigeUtbetaling, utbetaling ->
            forrigeUtbetaling != null && utbetaling == null
        }

    return erUtbetalingOpphørtTidslinje.perioder()
        .mapNotNull { erUtbetalingOpphørtPeriode ->
            if (erUtbetalingOpphørtPeriode.innhold == true) {
                Opphørsperiode(
                    periodeFom = erUtbetalingOpphørtPeriode.fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
                    periodeTom = erUtbetalingOpphørtPeriode.tilOgMed.tilDagEllerSisteDagIPerioden().tilLocalDate(),
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                )
            } else {
                null
            }
        }
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>, Måned> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { it.tilTidslinje() }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}

@JvmName("AndelTilkjentYtelseMedEndreteUtbetalingerTilTidslinje")
fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned> =
    this.map {
        Periode(
            fraOgMed = it.stønadFom.tilTidspunkt(),
            tilOgMed = it.stønadTom.tilTidspunkt(),
            innhold = it,
        )
    }.tilTidslinje()

private fun List<Utbetalingsperiode>.tilTidslinje() =
    this.map {
        Periode(
            fraOgMed = it.periodeFom.tilMånedTidspunkt(),
            tilOgMed = it.periodeTom.tilMånedTidspunkt(),
            innhold = it,
        )
    }.tilTidslinje()

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> =
    utbetalingsperioder.tilTidslinje().tilHarVerdiTidslinje().perioder()
        .filter { erUtbetalingIPeriode -> erUtbetalingIPeriode.innhold != true }
        .map {
            Opphørsperiode(
                periodeFom = it.fraOgMed.tilLocalDate(),
                periodeTom = it.tilOgMed.tilLocalDate(),
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
            )
        }

private fun <V, T : Tidsenhet> Tidslinje<V, T>.tilHarVerdiTidslinje(): Tidslinje<Boolean, T> =
    this.tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
        Innhold(this.innholdForTidspunkt(tidspunkt).innhold != null)
    }

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder
        .maxOfOrNull { it.periodeTom }?.toYearMonth()
        ?: TIDENES_ENDE.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(
            Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
            ),
        )
    } else {
        emptyList()
    }
}

fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
    if (alleOpphørsperioder.isEmpty()) return emptyList()

    val sortertOpphørsperioder = alleOpphørsperioder.sortedBy { it.periodeFom }

    return sortertOpphørsperioder.fold(
        mutableListOf(
            sortertOpphørsperioder.first(),
        ),
    ) { acc: MutableList<Opphørsperiode>, nesteOpphørsperiode: Opphørsperiode ->
        val forrigeOpphørsperiode = acc.last()
        when {
            nesteOpphørsperiode.periodeFom.isSameOrBefore(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
                acc[acc.lastIndex] =
                    forrigeOpphørsperiode.copy(
                        periodeTom = maxOfOpphørsperiodeTom(
                            forrigeOpphørsperiode.periodeTom,
                            nesteOpphørsperiode.periodeTom,
                        ),
                    )
            }

            else -> {
                acc.add(nesteOpphørsperiode)
            }
        }

        acc
    }
}

private fun maxOfOpphørsperiodeTom(a: LocalDate?, b: LocalDate?): LocalDate? {
    return if (a != null && b != null) maxOf(a, b) else null
}
