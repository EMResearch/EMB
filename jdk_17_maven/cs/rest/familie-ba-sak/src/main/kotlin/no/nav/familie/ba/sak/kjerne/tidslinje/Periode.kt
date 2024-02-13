package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange
import java.time.LocalDate
import java.time.YearMonth

data class Periode<I, T : Tidsenhet>(
    val fraOgMed: Tidspunkt<T>,
    val tilOgMed: Tidspunkt<T>,
    val innhold: I? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<T>, innhold: I?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}

fun <I> periodeAv(fraOgMed: LocalDate?, tilOgMed: LocalDate?, innhold: I): Periode<I, Dag> =
    Periode(fraOgMed.tilTidspunktEllerUendeligTidlig(), tilOgMed.tilTidspunktEllerUendeligSent(), innhold)

fun <I> månedPeriodeAv(fraOgMed: YearMonth?, tilOgMed: YearMonth?, innhold: I): Periode<I, Måned> =
    Periode(fraOgMed.tilTidspunktEllerUendeligTidlig(), tilOgMed.tilTidspunktEllerUendeligSent(), innhold)

fun <I, T : Tidsenhet> periodeAv(
    fraOgMed: Tidspunkt<T>,
    tilOgMed: Tidspunkt<T>,
    innhold: I,
): Periode<I, T> = Periode(fraOgMed, tilOgMed, innhold)

fun <I, T : Tidsenhet> Tidspunkt<T>.tilPeriodeMedInnhold(innhold: I?) = Periode(this, this, innhold)

fun <I, T : Tidsenhet> Tidspunkt<T>.tilPeriodeUtenInnhold() = tilPeriodeMedInnhold(null as I)

fun <I, T : Tidsenhet, R> Collection<Periode<I, T>>.mapInnhold(mapper: (I?) -> R?): Collection<Periode<R, T>> =
    this.map { Periode(it.fraOgMed, it.tilOgMed, mapper(it.innhold)) }
