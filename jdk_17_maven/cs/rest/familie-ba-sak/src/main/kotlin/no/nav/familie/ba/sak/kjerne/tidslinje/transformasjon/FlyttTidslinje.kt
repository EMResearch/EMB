package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet

/**
 * Extension-metode for å forskyve tidslinjen <tidsenheter> frem eller tilbake
 * Negative <tidsenheter> flytter tidslinjen tilbake
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.forskyv(tidsenheter: Long): Tidslinje<I, T> {
    val tidslinje = this

    return object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> =
            tidslinje.perioder().map {
                Periode(it.fraOgMed.flytt(tidsenheter), it.tilOgMed.flytt(tidsenheter), it.innhold)
            }
    }
}
