package no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth

fun <T : Tidsenhet> Tidspunkt<T>.tilFørsteDagIMåneden() = when (this) {
    is DagTidspunkt -> DagTidspunkt(this.dato.withDayOfMonth(1), uendelighet)
    is MånedTidspunkt -> DagTidspunkt(this.måned.atDay(1), uendelighet)
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilSisteDagIMåneden() = when (this) {
    is DagTidspunkt -> DagTidspunkt(this.dato.sisteDagIMåned(), uendelighet)
    is MånedTidspunkt -> DagTidspunkt(this.måned.atEndOfMonth(), uendelighet)
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilInneværendeMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet)
    is MånedTidspunkt -> this
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilNesteMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet).neste()
    is MånedTidspunkt -> this.neste()
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilForrigeMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet).forrige()
    is MånedTidspunkt -> this.forrige()
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilDagEllerFørsteDagIPerioden() = when (this) {
    is DagTidspunkt -> this
    is MånedTidspunkt -> DagTidspunkt(this.måned.atDay(1), this.uendelighet)
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.tilDagEllerSisteDagIPerioden() = when (this) {
    is DagTidspunkt -> this
    is MånedTidspunkt -> DagTidspunkt(this.måned.atEndOfMonth(), this.uendelighet)
    else -> throw Feil("Ukjent type tidspunkt")
}

fun <T : Tidsenhet> Tidspunkt<T>.neste() = flytt(1)
fun <T : Tidsenhet> Tidspunkt<T>.forrige() = flytt(-1)
fun <T : Tidsenhet> Tidspunkt<T>.erRettFør(tidspunkt: Tidspunkt<T>) = neste() == tidspunkt
fun <T : Tidsenhet> Tidspunkt<T>.erEndelig(): Boolean = uendelighet == Uendelighet.INGEN
fun <T : Tidsenhet> Tidspunkt<T>.erUendeligLengeSiden(): Boolean = uendelighet == Uendelighet.FORTID
fun <T : Tidsenhet> Tidspunkt<T>.erUendeligLengeTil(): Boolean = uendelighet == Uendelighet.FREMTID

fun <T : Tidsenhet> Tidspunkt<T>.somEndelig() = medUendelighet(Uendelighet.INGEN)
fun <T : Tidsenhet> Tidspunkt<T>.somUendeligLengeSiden() = medUendelighet(Uendelighet.FORTID)
fun <T : Tidsenhet> Tidspunkt<T>.somUendeligLengeTil() = medUendelighet(Uendelighet.FREMTID)
fun <T : Tidsenhet> Tidspunkt<T>.somFraOgMed() = when (uendelighet) {
    Uendelighet.FREMTID -> medUendelighet(Uendelighet.INGEN)
    else -> this
}

fun <T : Tidsenhet> Tidspunkt<T>.somTilOgMed() = when (uendelighet) {
    Uendelighet.FORTID -> medUendelighet(Uendelighet.INGEN)
    else -> this
}

fun <T : Tidsenhet> Tidspunkt<T>.tilYearMonth() = this.tilInneværendeMåned().tilYearMonth()
fun <T : Tidsenhet> Tidspunkt<T>.tilYearMonthEllerNull() = this.tilInneværendeMåned().tilYearMonthEllerNull()
fun <T : Tidsenhet> Tidspunkt<T>.tilYearMonthEllerUendeligFortid() = this.tilInneværendeMåned().tilYearMonthEllerNull() ?: TIDENES_MORGEN.toYearMonth()
fun <T : Tidsenhet> Tidspunkt<T>.tilYearMonthEllerUendeligFramtid() = this.tilInneværendeMåned().tilYearMonthEllerNull() ?: TIDENES_ENDE.toYearMonth()
fun <T : Tidsenhet> Tidspunkt<T>.tilLocalDate() = this.tilDagEllerSisteDagIPerioden().tilLocalDate()
fun <T : Tidsenhet> Tidspunkt<T>.tilLocalDateEllerNull() = this.tilDagEllerSisteDagIPerioden().tilLocalDateEllerNull()
