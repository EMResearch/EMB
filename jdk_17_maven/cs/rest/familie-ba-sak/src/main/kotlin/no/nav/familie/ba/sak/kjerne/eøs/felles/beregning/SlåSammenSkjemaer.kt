package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenBarn
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull

fun <T : PeriodeOgBarnSkjema<T>> Collection<T>.slåSammen(): Collection<T> {
    if (this.isEmpty()) {
        return this
    }

    val kompetanseSettTidslinje: Tidslinje<Set<T>, Måned> = this.map { it.tilTidslinje() }
        .kombinerUtenNull {
            it.groupingBy { it.utenBarn() }.reduce { _, acc, kompetanse -> acc.leggSammenBarn(kompetanse) }
                .values.toSet()
        }

    val kompetanserSlåttSammenVertikalt = kompetanseSettTidslinje.perioder().flatMap { periode ->
        periode.innhold?.settFomOgTom(periode) ?: emptyList()
    }

    val kompetanseSlåttSammenHorisontalt = kompetanserSlåttSammenVertikalt
        .groupBy { it.utenPeriode() }
        .mapValues { (_, kompetanser) -> kompetanser.tilTidslinje().slåSammenLike() }
        .mapValues { (_, tidslinje) -> tidslinje.perioder() }
        .values.flatten().mapNotNull { periode -> periode.innhold?.settFomOgTom(periode) }

    return kompetanseSlåttSammenHorisontalt
}

private fun <T : PeriodeOgBarnSkjema<T>> T.leggSammenBarn(kompetanse: T) =
    this.kopier(
        fom = this.fom,
        tom = this.tom,
        barnAktører = this.barnAktører + kompetanse.barnAktører,
    )

fun <T : PeriodeOgBarnSkjema<T>> Iterable<T>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { skjema -> skjema.settFomOgTom(periode) }

fun <T : PeriodeOgBarnSkjema<T>> T.settFomOgTom(periode: Periode<*, Måned>) =
    this.kopier(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull(),
        barnAktører = this.barnAktører,
    )
