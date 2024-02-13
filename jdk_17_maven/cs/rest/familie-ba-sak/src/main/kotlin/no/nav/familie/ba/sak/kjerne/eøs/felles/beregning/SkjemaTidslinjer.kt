package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull

fun <S : PeriodeOgBarnSkjema<S>> S.tilTidslinje() = listOf(this).tilTidslinje()

internal fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilTidslinje() =
    tidslinje {
        this.map {
            Periode(
                it.fom.tilTidspunktEllerUendeligTidlig(),
                it.tom.tilTidspunktEllerUendeligSent(),
                it.utenPeriode(),
            )
        }
    }

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<S, Måned>> {
    val skjemaer = this
    if (skjemaer.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = skjemaer.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        tidslinje {
            skjemaer
                .filter { it.barnAktører.contains(aktør) }
                .map {
                    Periode(
                        fraOgMed = it.fom.tilTidspunktEllerUendeligTidlig(it.tom),
                        tilOgMed = it.tom.tilTidspunktEllerUendeligSent(it.fom),
                        innhold = it.kopier(fom = null, tom = null, barnAktører = setOf(aktør)),
                    )
                }
        }
    }
}

fun <S : PeriodeOgBarnSkjemaEntitet<S>> Map<Aktør, Tidslinje<S, Måned>>.tilSkjemaer() =
    this.flatMap { (aktør, tidslinjer) -> tidslinjer.tilSkjemaer(aktør) }
        .slåSammen()

private fun <S : PeriodeOgBarnSkjema<S>> Tidslinje<S, Måned>.tilSkjemaer(aktør: Aktør) =
    this.perioder().mapNotNull { periode ->
        periode.innhold?.kopier(
            fom = periode.fraOgMed.tilYearMonthEllerNull(),
            tom = periode.tilOgMed.tilYearMonthEllerNull(),
            barnAktører = setOf(aktør),
        )
    }
