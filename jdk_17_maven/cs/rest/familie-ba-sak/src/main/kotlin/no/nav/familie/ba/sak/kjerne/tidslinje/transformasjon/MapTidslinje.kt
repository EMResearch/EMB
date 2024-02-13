package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.Innhold
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidslinjeFraTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

/**
 * Extension-metode for å map'e innhold fra en type og verdi til en annen
 * Hvis det nå oppstår tilgrensende perioder med samme innhold, slås de sammen
 */
fun <I, T : Tidsenhet, R> Tidslinje<I, T>.map(mapper: (I?) -> R?): Tidslinje<R, T> =
    tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
        val innholdsresultat = this.innholdForTidspunkt(tidspunkt)
        when (innholdsresultat.harInnhold) {
            false -> Innhold.utenInnhold()
            else -> Innhold<R>(mapper(innholdsresultat.innhold))
        }
    }

/**
 * Extension-metode for å map'e innhold som ikke er <null> fra en type og verdi til en annen
 * Hvis det nå oppstår tilgrensende perioder med samme innhold, slås de sammen
 */
fun <I, T : Tidsenhet, R> Tidslinje<I, T>.mapIkkeNull(mapper: (I) -> R?): Tidslinje<R, T> =
    tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
        val innholdsresultat = this.innholdForTidspunkt(tidspunkt)
        when (innholdsresultat.harInnhold) {
            false, (innholdsresultat.innhold == null) -> Innhold.utenInnhold()
            else -> Innhold<R>(mapper(innholdsresultat.innhold!!))
        }
    }
