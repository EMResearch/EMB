package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somTilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import java.time.YearMonth

class CharTidslinje<T : Tidsenhet>(private val tegn: String, private val startTidspunkt: Tidspunkt<T>) :
    Tidslinje<Char, T>() {

    val fraOgMed = when (tegn.first()) {
        '<' -> startTidspunkt.somUendeligLengeSiden()
        else -> startTidspunkt
    }

    val tilOgMed: Tidspunkt<T>
        get() {
            val sluttMåned = startTidspunkt.flytt(tegn.length.toLong() - 1)
            return when (tegn.last()) {
                '>' -> sluttMåned.somUendeligLengeTil()
                else -> sluttMåned
            }
        }

    override fun lagPerioder(): Collection<Periode<Char, T>> {
        val tidspunkter = fraOgMed..tilOgMed

        return tidspunkter.mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (tegn[index] == '<') tegn[index + 1] else tegn[index]
                tegn.length - 1 -> if (tegn[index] == '>') tegn[index - 1] else tegn[index]
                else -> tegn[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}

fun String.tilCharTidslinje(fom: YearMonth): Tidslinje<Char, Måned> =
    CharTidslinje(this, MånedTidspunkt.med(fom)).slåSammenLike()

fun <T : Tidsenhet> String.tilCharTidslinje(fom: Tidspunkt<T>): Tidslinje<Char, T> =
    CharTidslinje(this, fom).slåSammenLike()
