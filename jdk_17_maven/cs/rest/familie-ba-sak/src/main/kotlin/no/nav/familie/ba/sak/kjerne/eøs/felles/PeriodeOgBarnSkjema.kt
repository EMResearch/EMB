package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.erEkteDelmengdeAv
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth

interface PeriodeOgBarnSkjema<out T> where T : PeriodeOgBarnSkjema<T> {
    val fom: YearMonth?
    val tom: YearMonth?
    val barnAktører: Set<Aktør>
    fun utenInnhold(): T
    fun kopier(
        fom: YearMonth? = this.fom,
        tom: YearMonth? = this.tom,
        barnAktører: Set<Aktør> = this.barnAktører.map { it.copy() }.toSet(),
    ): T
}

fun <T : PeriodeOgBarnSkjema<T>> T.medBarnOgPeriodeSomOverlapperMed(skjema: T): T? {
    val fom = maxOf(this.fom ?: MIN_MÅNED, skjema.fom ?: MIN_MÅNED)
    val tom = minOf(this.tom ?: MAX_MÅNED, skjema.tom ?: MAX_MÅNED)

    val snitt = this.kopier(
        fom = if (fom == MIN_MÅNED) null else fom,
        tom = if (tom == MAX_MÅNED) null else tom,
        barnAktører = this.barnAktører.intersect(skjema.barnAktører),
    )

    return if (snitt.harBarnOgPeriode()) snitt else null
}

fun <T : PeriodeOgBarnSkjema<T>> T.harBarnOgPeriode(): Boolean {
    val harGyldigPeriode = fom == null || tom == null || fom!! <= tom
    return harGyldigPeriode && barnAktører.isNotEmpty()
}

fun <T : PeriodeOgBarnSkjema<T>> T.inneholder(skjema: T): Boolean {
    return this.bareInnhold() == skjema.bareInnhold() &&
        (this.fom == null || this.fom!! <= skjema.fom) &&
        (this.tom == null || this.tom!! >= skjema.tom) &&
        this.barnAktører.containsAll(skjema.barnAktører)
}

fun <T : PeriodeOgBarnSkjema<T>> T.bareInnhold(): T =
    this.kopier(fom = null, tom = null, barnAktører = emptySet())

fun <T : PeriodeOgBarnSkjema<T>> T.utenBarn(): T =
    this.kopier(fom = this.fom, tom = this.tom, barnAktører = emptySet())

fun <T : PeriodeOgBarnSkjema<T>> T.utenPeriode(): T =
    this.kopier(fom = null, tom = null, barnAktører = this.barnAktører)

fun <T : PeriodeOgBarnSkjema<T>> T.utenInnholdTilOgMed(tom: YearMonth?) =
    this.kopier(
        fom = this.tom?.plusMonths(1),
        tom = tom,
    ).utenInnhold()

fun <T : PeriodeOgBarnSkjema<T>> T.medBarnaSomForsvinnerFra(skjema: T): T =
    this.kopier(barnAktører = skjema.barnAktører.minus(this.barnAktører))

fun <T : PeriodeOgBarnSkjema<T>> T.tilOgMedBlirForkortetEllerLukketAv(skjema: T): Boolean =
    skjema.tom != null && (this.tom == null || this.tom!! > skjema.tom)

fun <T : PeriodeOgBarnSkjema<T>> T.erLikBortsettFraTilOgMed(skjema: T): Boolean =
    this.kopier(tom = skjema.tom) == skjema

fun <T : PeriodeOgBarnSkjema<T>> T.erLikBortsettFraBarn(skjema: T): Boolean =
    this.kopier(barnAktører = skjema.barnAktører) == skjema

fun <T : PeriodeOgBarnSkjema<T>> T.erLikBortsettFraBarnOgTilOgMed(skjema: T): Boolean =
    this.kopier(barnAktører = skjema.barnAktører, tom = skjema.tom) == skjema

fun <T : PeriodeOgBarnSkjema<T>> T.harEkteDelmengdeAvBarna(skjema: T): Boolean =
    this.barnAktører.erEkteDelmengdeAv(skjema.barnAktører)
