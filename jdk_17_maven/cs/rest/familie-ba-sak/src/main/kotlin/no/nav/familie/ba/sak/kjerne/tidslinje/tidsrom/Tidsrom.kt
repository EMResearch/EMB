package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.forrige
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo

fun <I, T : Tidsenhet> Tidslinje<I, T>.fraOgMed() =
    this.perioder().firstOrNull()?.let {
        if (it.fraOgMed.erEndelig()) {
            it.fraOgMed
        } else {
            minOf(it.fraOgMed.somEndelig(), it.tilOgMed.somEndelig()).somUendeligLengeSiden()
        }
    }

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilOgMed() =
    this.perioder().lastOrNull()?.let {
        if (it.tilOgMed.erEndelig()) {
            it.tilOgMed
        } else {
            maxOf(it.fraOgMed.somEndelig(), it.tilOgMed.somEndelig()).somUendeligLengeTil()
        }
    }

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.fraOgMed() = this
    .mapNotNull { it.fraOgMed() }
    .minsteEllerNull()

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tilOgMed() = this
    .mapNotNull { it.tilOgMed() }
    .størsteEllerNull()

fun <I, T : Tidsenhet> Tidslinje<I, T>.tidsrom(): Collection<Tidspunkt<T>> = when {
    this.perioder().isEmpty() -> emptyList()
    else -> (perioder().first().fraOgMed.rangeTo(perioder().last().tilOgMed)).toList()
}

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tidsrom(): Collection<Tidspunkt<T>> {
    val fraOgMed = fraOgMed() ?: return emptyList()
    val tilOgMed = tilOgMed() ?: return emptyList()
    return (fraOgMed..tilOgMed).toList()
}

fun <T : Tidsenhet> tidsrom(vararg tidslinjer: Tidslinje<*, T>) = tidslinjer.toList().tidsrom()

private fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.størsteEllerNull() =
    this.reduceOrNull { acc, neste -> størsteAv(acc, neste) }

private fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.minsteEllerNull() =
    this.reduceOrNull { acc, neste -> minsteAv(acc, neste) }

internal fun <T : Tidsenhet> størsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeTil() && t2.erEndelig() && t1.endeligMindreEllerLik(t2)) {
        t2.neste().somUendeligLengeTil()
    } else if (t2.erUendeligLengeTil() && t1.erEndelig() && t2.endeligMindreEllerLik(t1)) {
        t1.neste().somUendeligLengeTil()
    } else if (t1.erUendeligLengeTil() || t2.erUendeligLengeTil()) {
        maxOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeTil()
    } else {
        maxOf(t1, t2)
    }

internal fun <T : Tidsenhet> minsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeSiden() && t2.erEndelig() && t2.endeligMindreEllerLik(t1)) {
        t2.forrige().somUendeligLengeSiden()
    } else if (t2.erUendeligLengeSiden() && t1.erEndelig() && t1.endeligMindreEllerLik(t2)) {
        t1.forrige().somUendeligLengeSiden()
    } else if (t1.erUendeligLengeSiden() || t2.erUendeligLengeSiden()) {
        minOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeSiden()
    } else {
        minOf(t1, t2)
    }
