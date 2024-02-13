package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.forrige
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import java.time.LocalDate
import java.time.YearMonth

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> = tidslinje {
    listOf(
        Periode(
            fødselsdato.toYearMonth().tilTidspunkt().neste(),
            fødselsdato.plusYears(18).toYearMonth().tilTidspunkt().forrige(),
            true,
        ),
    )
}

fun erUnder6ÅrTidslinje(person: Person) = tidslinje {
    listOf(
        Periode(
            person.fødselsdato.toYearMonth().tilTidspunkt(),
            person.fødselsdato.toYearMonth().plusYears(6).tilTidspunkt().forrige(),
            true,
        ),
    )
}

fun erTilogMed3ÅrTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> = tidslinje {
    listOf(
        Periode(
            fødselsdato.toYearMonth().tilTidspunkt().neste(),
            fødselsdato.plusYears(3).toYearMonth().tilTidspunkt(),
            true,
        ),
    )
}

fun opprettBooleanTidslinje(fraÅrMåned: YearMonth, tilÅrMåned: YearMonth) = tidslinje {
    listOf(
        Periode(
            fraÅrMåned.tilTidspunkt(),
            tilÅrMåned.tilTidspunkt(),
            true,
        ),
    )
}
