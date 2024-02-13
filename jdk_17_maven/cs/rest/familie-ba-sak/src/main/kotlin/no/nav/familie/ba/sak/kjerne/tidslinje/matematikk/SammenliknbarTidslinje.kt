import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.joinIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet

fun <K, I : Comparable<I>, T : Tidsenhet> minsteAvHver(
    aTidslinjer: Map<K, Tidslinje<I, T>>,
    bTidslinjer: Map<K, Tidslinje<I, T>>,
) = aTidslinjer.joinIkkeNull(bTidslinjer) { a, b -> minOf(a, b) }
