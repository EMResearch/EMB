package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet

fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.outerJoin(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys + høyreTidslinjer.keys

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.leftJoin(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val venstreNøkler = venstreTidslinjer.keys

    return venstreNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

/**
 * Extension-metode for å kombinere to nøkkel-verdi-map'er der verdiene er tidslinjer
 * Nøkkelen må være av samme type, K, tidslinjene må være i samme tidsenhet (T)
 * Innholdet i tidslinjene i map'en på venstre side må alle være av typen V
 * Innholdet i tidslinjene i map'en på høyre side må alle være av typen H
 * Kombinator-funksjonen kalles med verdiene av fra venstre og høyre tidslinje for samme nøkkel og tidspunkt.
 * <null> blir sendt som verdier hvis venstre, høyre eller begge tidslinjer mangler verdi for et tidspunkt
 * Resultatet er en ny map der nøklene er av type K, og tidslinjene har innhold av typen (nullable) R.
 * Bare nøkler som finnes i begge map'ene vil finnes i den resulterende map'en
 */
fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.join(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys.intersect(høyreTidslinjer.keys)

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

/**
 * Extension-metode for å kombinere to nøkkel-verdi-map'er der verdiene er tidslinjer
 * Nøkkelen må være av samme type, K, tidslinjene må være i samme tidsenhet (T)
 * Innholdet i tidslinjene i map'en på venstre side må alle være av typen V
 * Innholdet i tidslinjene i map'en på høyre side må alle være av typen H
 * Kombinator-funksjonen kalles med verdiene av fra venstre og høyre tidslinje for samme nøkkel og tidspunkt.
 * Kombinator-funksjonen blir IKKE kalt Hvis venstre, høyre eller begge tidslinjer mangler verdi for et tidspunkt
 * Resultatet er en ny map der nøklene er av type K, og tidslinjene har innhold av typen (nullable) R.
 * Bare nøkler som finnes i begge map'ene vil finnes i den resulterende map'en
 */
fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.joinIkkeNull(
    høyreTidslinjer: Map<K, Tidslinje<H, T>>,
    kombinator: (V, H) -> R?,
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys.intersect(høyreTidslinjer.keys)

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, TomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, TomTidslinje())

        venstreTidslinje.kombinerUtenNullMed(høyreTidslinje, kombinator)
    }
}

/**
 * Extension-metode for å kombinere en nøkkel-verdi-map'er der verdiene er tidslinjer, med en enkelt tidslinje
 * Innholdet i tidslinjene i map'en på venstre side må alle være av typen V
 * Innholdet i tidslinjen på høyre side er av typen H
 * Kombinator-funksjonen kalles for hvert tidspunkt med med verdien for det tidspunktet fra høyre tidslinje og
 * vedien fra den enkelte av venstre tidslinjer etter tur.
 * Kombinator-funksjonen blir IKKE kalt Hvis venstre, høyre eller begge tidslinjer mangler verdi for et tidspunkt
 * Resultatet er en ny map der nøklene er av type K, og tidslinjene har innhold av typen (nullable) R.
 */
fun <K, V, H, R, T : Tidsenhet> Map<K, Tidslinje<V, T>>.kombinerKunVerdiMed(
    høyreTidslinje: Tidslinje<H, T>,
    kombinator: (V, H) -> R?,
): Map<K, Tidslinje<R, T>> {
    val venstreTidslinjer = this

    return venstreTidslinjer.mapValues { (_, venstreTidslinje) ->
        venstreTidslinje.kombinerUtenNullMed(høyreTidslinje, kombinator)
    }
}
