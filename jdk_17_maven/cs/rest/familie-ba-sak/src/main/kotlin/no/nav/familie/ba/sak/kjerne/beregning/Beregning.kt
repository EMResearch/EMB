package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

fun beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse: Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>): LocalDateTimeline<Int> {
    return andelerTilkjentYtelse
        .map { personTilTimeline(it) }
        .reduce(::reducer)
}

private fun personTilTimeline(it: AndelTilkjentYtelseMedEndreteUtbetalinger) =
    LocalDateTimeline(
        listOf(
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it.kalkulertUtbetalingsbeløp,
            ),
        ),
    )

private fun reducer(
    sammenlagtTidslinje: LocalDateTimeline<Int>,
    tidslinje: LocalDateTimeline<Int>,
): LocalDateTimeline<Int> {
    sammenlagtTidslinje.disjoint(tidslinje)
    return sammenlagtTidslinje.combine(
        tidslinje,
        StandardCombinators::sum,
        LocalDateTimeline.JoinStyle.CROSS_JOIN,
    )
}
