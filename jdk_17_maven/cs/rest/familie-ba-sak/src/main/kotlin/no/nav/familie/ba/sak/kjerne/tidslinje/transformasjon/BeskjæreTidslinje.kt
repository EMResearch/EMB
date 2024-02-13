package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidslinjeFraTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [tidslinje]s fraOgMed() og til [tidslinje]s tilOgMed()
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 * Beskjæring mot tom tidslinje vil gi tom tidslinje
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> = when {
    tidslinje.tidsrom().isEmpty() -> TomTidslinje()
    else -> beskjær(tidslinje.fraOgMed()!!, tidslinje.tilOgMed()!!)
}

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter til-og-med fra en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [this]s fraOgMed() og til [tidslinje]s tilOgMed()
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 * Beskjæring mot tom tidslinje vil gi tom tidslinje
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærTilOgMedEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> = when {
    tidslinje.tidsrom().isEmpty() -> TomTidslinje()
    else -> beskjær(this.fraOgMed()!!, tidslinje.tilOgMed()!!)
}

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til [tilOgMed]
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Uendelige endepunkter vil beskjæres til endelig hvis [fraOgMed] eller [tilOgMed] er endelige
 * Endelige endepunkter som beskjæres mot uendelige endepunkter, beholdes
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den mest ekstreme benyttes
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjær(fraOgMed: Tidspunkt<T>, tilOgMed: Tidspunkt<T>): Tidslinje<I, T> {
    if (tidsrom().isEmpty()) {
        return this
    }

    val fom: Tidspunkt<T> = when {
        // <--A..F begrenset med <--C..F må sjekke verdier fra og med <--A
        // <--C..F begrenset med <--A..F trenger bare å sjekke verdier fra og med <--C
        // Dvs i tilfellet de tidslinjens fom og begrensningens fom begge peker bakover, skal tidslinjens fom brukes
        fraOgMed()!!.erUendeligLengeSiden() && fraOgMed.erUendeligLengeSiden() -> this.fraOgMed()!!
        else -> maxOf(fraOgMed()!!, fraOgMed)
    }
    val tom: Tidspunkt<T> = when {
        // A..F--> begrenset med A..C--> må sjekke verdier frem til og med F-->
        // A..C--> begrenset med A..F--> trenger bare å sjekke verdier frem til og med C-->
        // Dvs i tilfellet de tidslinjens tom og begrensningens tom begge peker fremover, skal tidslinjens tom brukes
        tilOgMed()!!.erUendeligLengeTil() && tilOgMed.erUendeligLengeTil() -> this.tilOgMed()!!
        else -> minOf(tilOgMed()!!, tilOgMed)
    }

    return (fom..tom).tidslinjeFraTidspunkt { tidspunkt -> innholdForTidspunkt(tidspunkt) }
}
