package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo

fun <T : Tidsenhet, I> TidspunktClosedRange<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> {
    val fom = this.start
    val tom = this.endInclusive
    return object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> {
            return listOf(Periode(fom, tom, innhold()))
        }
    }
}

infix fun <T : Tidsenhet, I> TidspunktClosedRange<T>.med(innhold: () -> I): Tidslinje<I, T> = this.tilTidslinje(innhold)

fun <T : Tidsenhet, I> Tidspunkt<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> =
    this.rangeTo(this).tilTidslinje(innhold)
