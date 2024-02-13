package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.inneholder
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED

/**
 * Reduser innholdet i this-kompetansen med innholdet i oppdaterKompetanse
 * En viktig forutsetning er at oppdatertKompetanse alltid er "mindre" enn kompetansen som reduseres
 */
fun <T : PeriodeOgBarnSkjema<T>> T.trekkFra(skjema: T): Collection<T> {
    val gammeltSkjema = this
    val skjemaForRestBarn = gammeltSkjema
        .kopier(
            barnAktører = gammeltSkjema.barnAktører.minus(skjema.barnAktører),
        ).takeIf { it.barnAktører.isNotEmpty() }

    val skjemaForForegåendePerioder = gammeltSkjema
        .kopier(
            fom = gammeltSkjema.fom,
            tom = skjema.fom?.minusMonths(1),
            barnAktører = skjema.barnAktører,
        ).takeIf { it.fom != null && it.fom!! <= it.tom }

    val skjemaForEtterfølgendePerioder = gammeltSkjema.kopier(
        fom = skjema.tom?.plusMonths(1),
        tom = gammeltSkjema.tom,
        barnAktører = skjema.barnAktører,
    ).takeIf { it.fom != null && it.fom!! <= (it.tom ?: MAX_MÅNED) }

    return listOfNotNull(skjemaForRestBarn, skjemaForForegåendePerioder, skjemaForEtterfølgendePerioder)
}

fun <T : PeriodeOgBarnSkjema<T>> Iterable<T>.trekkFra(skalFjernes: T) =
    this.flatMap { skjema ->
        if (skjema.inneholder(skalFjernes)) {
            skjema.trekkFra(skalFjernes)
        } else {
            listOf(skjema)
        }
    }
