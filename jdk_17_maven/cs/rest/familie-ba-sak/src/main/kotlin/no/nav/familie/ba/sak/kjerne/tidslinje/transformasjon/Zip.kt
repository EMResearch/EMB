package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.ZipPadding.ETTER
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.ZipPadding.FØR
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.ZipPadding.INGEN_PADDING
import java.time.YearMonth

/**
 * Returnerer en tidslinje med par av hvert etterfølgende element i tidslinjen.
 *
 * val aTilD = "abcd"
 * val bokstavTidslinje = aTilF.tilCharTidslinje(jan(2020))
 * val bokstavParTidslinje = bokstavTidslinje.zipMedNeste(ZipPadding.FØR)
 *
 * println(bokstavTidslinje) //
 *     2020-01 - 2020-01: a | 2020-02 - 2020-02: b | 2020-03 - 2020-03: c | 2020-04 - 2020-04: d
 *
 * println(bokstavParTidslinje) //
 *     2020-01 - 2020-01: (null, a) | 2020-02 - 2020-02: (a, b) | 2020-03 - 2020-03: (b, c) | 2020-04 - 2020-04: (c, d)
 */
enum class ZipPadding {
    FØR,
    ETTER,
    INGEN_PADDING,
}

fun <T> Tidslinje<T, Måned>.zipMedNeste(zipPadding: ZipPadding = INGEN_PADDING): Tidslinje<Pair<T?, T?>, Måned> {
    val padding = listOf(
        månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
    )

    return when (zipPadding) {
        FØR -> padding + perioder()
        ETTER -> perioder() + padding
        INGEN_PADDING -> perioder()
    }.zipWithNext { forrige, denne ->
        periodeAv(denne.fraOgMed, denne.tilOgMed, Pair(forrige.innhold, denne.innhold))
    }.tilTidslinje()
}
