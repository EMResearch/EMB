package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet

class TomTidslinje<I, T : Tidsenhet> : Tidslinje<I, T>() {
    override fun lagPerioder(): Collection<Periode<I, T>> = emptyList()
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.erTom() = this == TomTidslinje<I, T>()
fun <I, T : Tidsenhet> Tidslinje<I, T>.erIkkeTom() = !this.erTom()
