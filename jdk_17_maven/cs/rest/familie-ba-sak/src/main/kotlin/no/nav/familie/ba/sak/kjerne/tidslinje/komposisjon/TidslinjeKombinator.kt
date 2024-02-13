package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste fraOgMed() til største tilOgMed() fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn (nullable) av V og H og returnerer (nullable) R
 * Kombinator-funksjonen blir ikke kalt hvis begge tidslinjene mangler innhold for tidspunktet
 * Hvis kombinator-funksjonen returner <null>, antas det at tidslinjen ikke skal ha verdi for tidspunktet
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.kombinerMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (V?, H?) -> R?,
): Tidslinje<R, T> = tidsrom(this, høyreTidslinje).tidslinjeFraTidspunkt { tidspunkt ->
    val venstre = this.innholdForTidspunkt(tidspunkt)
    val høyre = høyreTidslinje.innholdForTidspunkt(tidspunkt)

    when {
        !(venstre.harInnhold || høyre.harInnhold) -> Innhold.utenInnhold()
        else -> kombinator(venstre.innhold, høyre.innhold).tilInnhold()
    }
}

/**
 * Extension-metode for å kombinere to tidslinjer der begge har verdi
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Hvis innholdet V eller H mangler innhold, så vil ikke resulterende tidslinje få innhold for det tidspunktet
 * Kombintor-funksjonen tar ellers V og H og returnerer (nullable) R
 * Hvis kombinator-funksjonen returner <null>, antas det at tidslinjen ikke skal ha verdi for tidspunktet
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.kombinerUtenNullMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (V, H) -> R?,
): Tidslinje<R, T> = tidsrom(this, høyreTidslinje).tidslinjeFraTidspunkt { tidspunkt ->
    val venstre = this.innholdForTidspunkt(tidspunkt)
    val høyre = høyreTidslinje.innholdForTidspunkt(tidspunkt)

    when {
        venstre.harVerdi && høyre.harVerdi -> kombinator(venstre.verdi, høyre.verdi).tilVerdi()
        else -> Innhold.utenInnhold()
    }
}

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Null-verdier fjernes før de sendes til kombinator-funksjonen, som betyr at en tom iterator kan bli sendt
 * Hvis reesultatet fra kombinatoren er null, tolkes det som at det ikke skal være innhold
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombinerUtenNull(
    listeKombinator: (Iterable<I>) -> R?,
): Tidslinje<R, T> = tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
    this.map { it.innholdForTidspunkt(tidspunkt) }
        .filter { it.harVerdi }
        .map { it.verdi }
        .let(listeKombinator).tilVerdi()
}

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Null-verdier fjernes, og listen av verdier sendes til kombinator-funksjonen bare hvis den inneholder verdier
 * Hvis reesultatet fra kombinatoren er null, tolkes det som at det ikke skal være innhold
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombinerUtenNullOgIkkeTom(
    listeKombinator: (Iterable<I>) -> R?,
): Tidslinje<R, T> = tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
    this.map { it.innholdForTidspunkt(tidspunkt) }
        .filter { it.harVerdi }
        .map { it.verdi }
        .takeIf { it.isNotEmpty() }
        ?.let(listeKombinator).tilVerdi()
}

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombiner(
    listeKombinator: (Iterable<I>) -> R?,
): Tidslinje<R, T> = tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
    this.map { it.innholdForTidspunkt(tidspunkt) }
        .filter { it.harVerdi }
        .map { it.verdi }
        .let { listeKombinator(it) }
        .tilVerdi()
}

/**
 * Extension-metode for å kombinere to tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Kombintor-funksjonen tar inn tidspunktet og (nullable) av V og H og returnerer (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.tidspunktKombinerMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (Tidspunkt<T>, V?, H?) -> R?,
): Tidslinje<R, T> = tidsrom(this, høyreTidslinje).tidslinjeFraTidspunkt { tidspunkt ->
    kombinator(
        tidspunkt,
        this.innholdForTidspunkt(tidspunkt).innhold,
        høyreTidslinje.innholdForTidspunkt(tidspunkt).innhold,
    ).tilInnhold()
}

/**
 * Extension-metode for å kombinere tre tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv A, B og C
 * Kombintor-funksjonen tar inn (nullable) av A, B og C og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <A, B, C, R, T : Tidsenhet> Tidslinje<A, T>.kombinerMed(
    tidslinjeB: Tidslinje<B, T>,
    tidslinjeC: Tidslinje<C, T>,
    kombinator: (A?, B?, C?) -> R?,
): Tidslinje<R, T> = tidsrom(this, tidslinjeB, tidslinjeC).tidslinjeFraTidspunkt { tidspunkt ->
    kombinator(
        this.innholdForTidspunkt(tidspunkt).innhold,
        tidslinjeB.innholdForTidspunkt(tidspunkt).innhold,
        tidslinjeC.innholdForTidspunkt(tidspunkt).innhold,
    ).tilInnhold()
}

/**
 * Extension-metode for å kombinere tre tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv A, B og C
 * Kombintor-funksjonen tar inn (nullable) av A, B og C og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <A, B, C, R, T : Tidsenhet> Tidslinje<A, T>.kombinerMedDatert(
    tidslinjeB: Tidslinje<B, T>,
    tidslinjeC: Tidslinje<C, T>,
    kombinator: (A?, B?, C?, Tidspunkt<T>) -> R?,
): Tidslinje<R, T> = tidsrom(this, tidslinjeB, tidslinjeC).tidslinjeFraTidspunkt { tidspunkt ->
    kombinator(
        this.innholdForTidspunkt(tidspunkt).innhold,
        tidslinjeB.innholdForTidspunkt(tidspunkt).innhold,
        tidslinjeC.innholdForTidspunkt(tidspunkt).innhold,
        tidspunkt,
    ).tilInnhold()
}

fun <A, B, C, R, T : Tidsenhet> Tidslinje<A, T>.kombinerMedKunVerdi(
    tidslinjeB: Tidslinje<B, T>,
    tidslinjeC: Tidslinje<C, T>,
    kombinator: (A, B, C) -> R?,
): Tidslinje<R, T> = tidsrom(this, tidslinjeB, tidslinjeC).tidslinjeFraTidspunkt { tidspunkt ->
    val innholdA = this.innholdForTidspunkt(tidspunkt)
    val innholdB = tidslinjeB.innholdForTidspunkt(tidspunkt)
    val innholdC = tidslinjeC.innholdForTidspunkt(tidspunkt)

    when {
        innholdA.harVerdi && innholdB.harVerdi && innholdC.harVerdi ->
            kombinator(innholdA.verdi, innholdB.verdi, innholdC.verdi).tilVerdi()
        else -> Innhold.utenInnhold()
    }
}

fun <V, H, T : Tidsenhet> Tidslinje<V, T>.harOverlappMed(tidslinje: Tidslinje<H, T>) =
    this.kombinerUtenNullMed(tidslinje) { v, h -> true }.erIkkeTom()

fun <V, H, T : Tidsenhet> Tidslinje<V, T>.harIkkeOverlappMed(tidslinje: Tidslinje<H, T>) =
    !this.harOverlappMed(tidslinje)

fun <V, H, T : Tidsenhet> Tidslinje<V, T>.kombinerMedNullable(
    høyreTidslinje: Tidslinje<H, T>?,
    kombinator: (V?, H?) -> V?,
): Tidslinje<V, T> = if (høyreTidslinje != null) { kombinerMed(høyreTidslinje, kombinator) } else this
