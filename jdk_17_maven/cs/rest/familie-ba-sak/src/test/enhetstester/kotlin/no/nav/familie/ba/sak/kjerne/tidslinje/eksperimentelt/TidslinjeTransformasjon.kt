package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

/**
 * Extension-metode for å kombinere et "vindu" med størrelse size i hver periode
 * Fungerer helt likt som (og bruker) Iterable.windowed
 * mapper-nmetoden må passe på ikke å skape overlapp mellom perioder (gir exception)
 * Forsøk på å flytte perioder utenfor tidslinjen, vil gi exception
 */
fun <I, T : Tidsenhet, R> Tidslinje<I, T>.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    mapper: (List<Periode<I, T>>) -> Periode<R, T>,
): Tidslinje<R, T> {
    val tidslinje = this

    return object : Tidslinje<R, T>() {
        val fraOgMed = tidslinje.fraOgMed()
        val tilOgMed = tidslinje.tilOgMed()

        override fun lagPerioder(): Collection<Periode<R, T>> =
            tidslinje.perioder().windowed(size, step, partialWindows) { perioder ->
                val periode = mapper(perioder)
                if (periode.fraOgMed < fraOgMed!! || periode.tilOgMed > tilOgMed!!) {
                    throw IllegalArgumentException("Forsøk på å flytte perioden utenfor grensene for tidslinjen")
                }
                periode
            }
    }
}
