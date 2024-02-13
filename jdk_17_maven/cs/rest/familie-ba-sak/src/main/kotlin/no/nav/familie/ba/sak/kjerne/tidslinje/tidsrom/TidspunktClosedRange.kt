package no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil

/**
 * ClosedRange-implementasjon for <Tidspunkt>, som gir <A..B>-operatoren.
 * Håndterer at <Tidspunkt> har en uendelighetskomponent; uendelig FORTID, uendelig FREMTID, eller ingen INGEN uendelighet
 * Her brukes følgende notasjon:
 * A,B,C - tidspunkt uten uendelighet. B er tidspunktet 1 tidsenhet etter A, og C er 1 tidsenhet etter B
 * <--A - tidspunkt som peker bakover mot uendelig fortid
 * B--> - tidspunkt som peker fremover mot uendelig fremtid
 * Følgende gjelder:
 * A..A = [A]
 * A..B = [A,B]
 * A..C = [A,B,C]
 * B..A = []
 * <--A..A = [<--A]
 * <--A..B = [<--A,B]
 * <--A..<--A = [<--A]
 * <--A..<--C = [<--A,B,C]
 * <--B..A = [<--A]
 * <--B..<--A = [<--A]
 * A..A--> = [A-->]
 * A-->..A--> = [A-->]
 * A-->..C--> = [A,B,C-->]
 * B..A--> = [B-->]
 * B-->..A--> = [B-->]
 * <--A..A--> = [<--A,B-->]
 * <--A..B--> = [<--A,B-->]
 * <--A..C--> = [<--A,B,C-->]
 * <--B..A--> = [<--A,B-->]
 * <--E..A--> = [<--A,B,C,D,E-->]
 * A-->..<--A = [A]
 * A-->..<--B = [A,B]
 * A-->..<--E = [A,B,C,D,E]
 * B-->..<--A = []
 */
data class TidspunktClosedRange<T : Tidsenhet>(
    override val start: Tidspunkt<T>,
    override val endInclusive: Tidspunkt<T>,
) : Iterable<Tidspunkt<T>>,
    ClosedRange<Tidspunkt<T>> {

    override fun toString(): String =
        "$start - $endInclusive"

    override fun iterator(): Iterator<Tidspunkt<T>> = object : Iterator<Tidspunkt<T>> {
        private var tidspunkt = when {
            // Bruk tidligste mulige ankerpunkt hvis start peker bakocer
            // Dvs <--C..A blir til <--A..A
            start.erUendeligLengeSiden() -> minOf(start.somEndelig(), endInclusive.somEndelig())
                .somUendeligLengeSiden()
            else -> start
        }

        private var tilOgMed = when {
            // Bruk seneste mulige ankerpunkt hvis endInclusive peker fremover
            // Dvs C..A--> blir til C..C-->
            endInclusive.erUendeligLengeTil() -> maxOf(start.somEndelig(), endInclusive.somEndelig())
                .somUendeligLengeTil()
            else -> endInclusive
        }

        override fun hasNext() = tidspunkt.endeligMindreEllerLik(tilOgMed)

        override fun next(): Tidspunkt<T> {
            return when {
                // Håndter spesialtilfellet <--A..A-->
                tidspunkt.erUendeligLengeSiden() && tilOgMed.erUendeligLengeTil() &&
                    tidspunkt.endeligLik(tilOgMed) ->
                    tidspunkt.also {
                        // Flytter gjeldnde tidspunkt til B-->, og må flytte tilOgMed tilsvarende for å få det med.
                        tidspunkt = tilOgMed.neste()
                        tilOgMed = tidspunkt
                    }

                // Hvis fraOgMed peker fremover, A-->, og tilOgMed peker bakover, <--C,
                // skal vi generere de endelige tidspunktene i overlappen mellom dem, her [A,B,C]
                tidspunkt.erUendeligLengeTil() && tilOgMed.erUendeligLengeSiden() ->
                    tidspunkt.somEndelig().also { tidspunkt = it.neste() }

                // Håndter tilfellet der tilOgMed = C-->, og tidspunkt er fremme ved C.
                // Da returrnes tilOgMed, altså C-->. Tidspunkt flyttes én frem, til D-->, som avslutter iterasjonen
                tilOgMed.erUendeligLengeTil() && tidspunkt.endeligLik(tilOgMed) ->
                    tilOgMed.also { tidspunkt = it.neste() }

                // Hvis tidspunkt = fraOgMed = A--> og tilOgMed = C-->
                // så returnes A, og tidspunkt settes til neste endelige tidspunkt, B
                tidspunkt.erUendeligLengeTil() && tilOgMed.erUendeligLengeTil() &&
                    tidspunkt.endeligMindre(tilOgMed) ->
                    tidspunkt.somEndelig().also { tidspunkt = it.neste() }

                // Ellers returner vi tidspunkt, og forvisser om at tidspunkt endelig og flytter det én tidsenhet frem
                else -> tidspunkt.also { tidspunkt = it.somEndelig().neste() }
            }
        }
    }
}

operator fun <T : Tidsenhet> Tidspunkt<T>.rangeTo(tilOgMed: Tidspunkt<T>): TidspunktClosedRange<T> =
    TidspunktClosedRange(this, tilOgMed)
