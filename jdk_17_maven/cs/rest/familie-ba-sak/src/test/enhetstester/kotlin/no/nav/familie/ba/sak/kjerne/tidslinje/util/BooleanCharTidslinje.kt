package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map

fun <T : Tidsenhet> String.somBoolskTidslinje(t: Tidspunkt<T>) = this.tilCharTidslinje(t).somBoolsk()

fun <T : Tidsenhet> Tidslinje<Char, T>.somBoolsk() = this.map {
    when (it?.lowercaseChar()) {
        't' -> true
        'f' -> false
        else -> null
    }
}
