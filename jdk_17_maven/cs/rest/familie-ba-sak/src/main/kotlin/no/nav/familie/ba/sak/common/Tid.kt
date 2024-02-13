package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.NavigableMap
import java.util.TreeMap

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX

private val FORMAT_DATE_DDMMYY = DateTimeFormatter.ofPattern("ddMMyy", nbLocale)
private val FORMAT_DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd", nbLocale)
private val FORMAT_DATO_NORSK_KORT_ÅR = DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale)
private val FORMAT_DATO_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy", nbLocale)
private val FORMAT_DATO_MÅNED_ÅR_KORT = DateTimeFormatter.ofPattern("MM.yy", nbLocale)
private val FORMAT_DATO_DAG_MÅNED_ÅR = DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale)
private val FORMAT_DATO_MÅNED_ÅR = DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale)

fun LocalDate.tilddMMyy() = this.format(FORMAT_DATE_DDMMYY)
fun LocalDate.tilyyyyMMdd() = this.format(FORMAT_DATE_ISO)
fun LocalDate.tilKortString() = this.format(FORMAT_DATO_NORSK_KORT_ÅR)
fun LocalDate.tilddMMyyyy() = this.format(FORMAT_DATO_NORSK)
fun YearMonth.tilKortString() = this.format(FORMAT_DATO_MÅNED_ÅR_KORT)
fun LocalDate.tilDagMånedÅr() = this.format(FORMAT_DATO_DAG_MÅNED_ÅR)
fun LocalDate.tilMånedÅr() = this.format(FORMAT_DATO_MÅNED_ÅR)
fun YearMonth.tilMånedÅr() = this.format(FORMAT_DATO_MÅNED_ÅR)

fun erBack2BackIMånedsskifte(tilOgMed: LocalDate?, fraOgMed: LocalDate?): Boolean {
    return tilOgMed?.erDagenFør(fraOgMed) == true &&
        tilOgMed.toYearMonth() != fraOgMed?.toYearMonth()
}

fun LocalDate.sisteDagIForrigeMåned(): LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.toYearMonth() = YearMonth.from(this)
fun YearMonth.toLocalDate() = LocalDate.of(this.year, this.month, 1)

fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)
fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()

fun LocalDate.forrigeMåned(): YearMonth {
    return this.toYearMonth().minusMonths(1)
}

fun YearMonth.forrigeMåned(): YearMonth {
    return this.minusMonths(1)
}

fun LocalDate.nesteMåned(): YearMonth {
    return this.toYearMonth().plusMonths(1)
}

fun YearMonth.nesteMåned(): YearMonth {
    return this.plusMonths(1)
}

fun inneværendeMåned(): YearMonth {
    return now().toYearMonth()
}

fun senesteDatoAv(dato1: LocalDate, dato2: LocalDate): LocalDate {
    return maxOf(dato1, dato2)
}

fun LocalDate.til18ÅrsVilkårsdato() = this.plusYears(18).minusDays(1)

fun LocalDate.sisteDagIMåned(): LocalDate {
    return YearMonth.from(this).atEndOfMonth()
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)
fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

fun LocalDate.erSenereEnnInneværendeMåned(): Boolean = this.isAfter(now().sisteDagIMåned())

fun LocalDate.erDagenFør(other: LocalDate?) = other != null && this.plusDays(1).equals(other)

fun LocalDate.erFraInneværendeMåned(now: LocalDate = now()): Boolean {
    val førsteDatoInneværendeMåned = now.withDayOfMonth(1)
    val førsteDatoNesteMåned = førsteDatoInneværendeMåned.plusMonths(1)
    return this.isSameOrAfter(førsteDatoInneværendeMåned) && isBefore(førsteDatoNesteMåned)
}

fun LocalDate.erFraInneværendeEllerForrigeMåned(now: LocalDate = now()): Boolean {
    val førsteDatoForrigeMåned = now.withDayOfMonth(1).minusMonths(1)
    val førsteDatoNesteMåned = førsteDatoForrigeMåned.plusMonths(2)
    return this.isSameOrAfter(førsteDatoForrigeMåned) && isBefore(førsteDatoNesteMåned)
}

fun YearMonth.isSameOrBefore(toCompare: YearMonth): Boolean {
    return this.isBefore(toCompare) || this == toCompare
}

fun YearMonth.isSameOrAfter(toCompare: YearMonth): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

fun LocalDate.isSameOrBefore(toCompare: LocalDate): Boolean {
    return this.isBefore(toCompare) || this == toCompare
}

fun LocalDate.isSameOrAfter(toCompare: LocalDate): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

fun LocalDate.isBetween(toCompare: Periode): Boolean {
    return this.isSameOrAfter(toCompare.fom) && this.isSameOrBefore(toCompare.tom)
}

fun Periode.overlapperHeltEllerDelvisMed(annenPeriode: Periode) =
    this.fom.isBetween(annenPeriode) ||
        this.tom.isBetween(annenPeriode) ||
        annenPeriode.fom.isBetween(this) ||
        annenPeriode.tom.isBetween(this)

fun MånedPeriode.inkluderer(yearMonth: YearMonth) = yearMonth >= this.fom && yearMonth <= this.tom

fun MånedPeriode.overlapperHeltEllerDelvisMed(annenPeriode: MånedPeriode) =
    this.inkluderer(annenPeriode.fom) ||
        this.inkluderer(annenPeriode.tom) ||
        annenPeriode.inkluderer(this.fom) ||
        annenPeriode.inkluderer(this.tom)

fun MånedPeriode.erMellom(annenPeriode: MånedPeriode) =
    annenPeriode.inkluderer(this.fom) && annenPeriode.inkluderer(this.tom)

fun Periode.kanErstatte(other: Periode): Boolean {
    return this.fom.isSameOrBefore(other.fom) && this.tom.isSameOrAfter(other.tom)
}

fun LocalDate.erMellomIkkeLik(other: Periode): Boolean {
    return this.isAfter(other.fom) && this.isBefore(other.tom)
}

fun Periode.kanSplitte(other: Periode): Boolean {
    return this.fom.erMellomIkkeLik(other) && this.tom.erMellomIkkeLik(other) &&
        (this.tom != TIDENES_ENDE || other.tom != TIDENES_ENDE)
}

fun Periode.kanFlytteFom(other: Periode): Boolean {
    return this.fom.isSameOrBefore(other.fom) && this.tom.isBetween(other)
}

fun Periode.kanFlytteTom(other: Periode): Boolean {
    return this.fom.isBetween(other) && this.tom.isSameOrAfter(other.tom)
}

fun Periode.tilMånedPeriode(): MånedPeriode = MånedPeriode(fom = this.fom.toYearMonth(), tom = this.tom.toYearMonth())

data class Periode(val fom: LocalDate, val tom: LocalDate)

data class MånedPeriode(val fom: YearMonth, val tom: YearMonth)
data class NullablePeriode(val fom: LocalDate?, val tom: LocalDate?) {
    fun tilNullableMånedPeriode() = NullableMånedPeriode(fom?.toYearMonth(), tom?.toYearMonth())
}

data class NullableMånedPeriode(val fom: YearMonth?, val tom: YearMonth?)

fun VilkårResultat.erEtterfølgendePeriode(other: VilkårResultat): Boolean {
    return (other.toPeriode().fom.monthValue - this.toPeriode().tom.monthValue <= 1) &&
        this.toPeriode().tom.year == other.toPeriode().fom.year
}

fun lagOgValiderPeriodeFraVilkår(
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): Periode {
    return when {
        periodeFom !== null -> {
            Periode(
                fom = periodeFom,
                tom = periodeTom ?: TIDENES_ENDE,
            )
        }

        erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
            Periode(
                fom = TIDENES_MORGEN,
                tom = TIDENES_ENDE,
            )
        }

        else -> {
            throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
        }
    }
}

fun RestVilkårResultat.toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
    this.periodeFom,
    this.periodeTom,
    this.erEksplisittAvslagPåSøknad,
)

fun VilkårResultat.toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
    this.periodeFom,
    this.periodeTom,
    this.erEksplisittAvslagPåSøknad,
)

fun DatoIntervallEntitet.erInnenfor(dato: LocalDate): Boolean {
    return when {
        fom == null && tom == null -> true
        fom == null -> dato.isSameOrBefore(tom!!)
        tom == null -> dato.isSameOrAfter(fom)
        else -> dato.isSameOrAfter(fom) && dato.isSameOrBefore(tom)
    }
}

fun slåSammenOverlappendePerioder(input: Collection<DatoIntervallEntitet>): List<DatoIntervallEntitet> {
    val map: NavigableMap<LocalDate, LocalDate?> =
        TreeMap()
    for (periode in input) {
        if (periode.fom != null &&
            (!map.containsKey(periode.fom) || periode.tom == null || periode.tom.isAfter(map[periode.fom]))
        ) {
            map[periode.fom] = periode.tom
        }
    }
    val result = mutableListOf<DatoIntervallEntitet>()
    var prevIntervall: DatoIntervallEntitet? = null
    for ((key, value) in map) {
        prevIntervall = if (prevIntervall != null && prevIntervall.erInnenfor(key)) {
            val fom = prevIntervall.fom
            val tom = if (prevIntervall.tom == null) {
                null
            } else {
                if (value != null && prevIntervall.tom!!.isAfter(value)) {
                    prevIntervall.tom
                } else {
                    value
                }
            }
            result.remove(prevIntervall)
            val nyttIntervall = DatoIntervallEntitet(fom, tom)
            result.add(nyttIntervall)
            nyttIntervall
        } else {
            val nyttIntervall = DatoIntervallEntitet(key, value)
            result.add(nyttIntervall)
            nyttIntervall
        }
    }
    return result
}

class YearMonthIterator(
    startMåned: YearMonth,
    val tilOgMedMåned: YearMonth,
    val hoppMåneder: Long,
) : Iterator<YearMonth> {

    private var gjeldendeMåned = startMåned

    override fun hasNext() =
        if (hoppMåneder > 0) {
            gjeldendeMåned.plusMonths(hoppMåneder) <= tilOgMedMåned.plusMonths(1)
        } else if (hoppMåneder < 0) {
            gjeldendeMåned.plusMonths(hoppMåneder) >= tilOgMedMåned.plusMonths(-1)
        } else {
            throw IllegalStateException("Steglengde kan ikke være null")
        }

    override fun next(): YearMonth {
        val next = gjeldendeMåned
        gjeldendeMåned = gjeldendeMåned.plusMonths(hoppMåneder)
        return next
    }
}

class YearMonthProgression(
    override val start: YearMonth,
    override val endInclusive: YearMonth,
    val hoppMåneder: Long = 1,
) : Iterable<YearMonth>,
    ClosedRange<YearMonth> {

    override fun iterator(): Iterator<YearMonth> =
        YearMonthIterator(start, endInclusive, hoppMåneder)

    infix fun step(måneder: Long) = YearMonthProgression(start, endInclusive, måneder)
}

operator fun YearMonth.rangeTo(andre: YearMonth) = YearMonthProgression(this, andre)
