package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange
import java.time.LocalDate
import java.time.YearMonth

fun jan(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 1))
fun feb(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 2))
fun mar(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 3))
fun apr(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 4))
fun mai(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 5))
fun jun(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 6))
fun jul(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 7))
fun aug(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 8))
fun sep(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 9))
fun okt(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 10))
fun nov(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 11))
fun des(år: Int) = MånedTidspunkt.med(YearMonth.of(år, 12))

fun Int.jan(år: Int) = DagTidspunkt.med(LocalDate.of(år, 1, this))
fun Int.feb(år: Int) = DagTidspunkt.med(LocalDate.of(år, 2, this))
fun Int.mar(år: Int) = DagTidspunkt.med(LocalDate.of(år, 3, this))
fun Int.apr(år: Int) = DagTidspunkt.med(LocalDate.of(år, 4, this))
fun Int.mai(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.jun(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.jul(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.aug(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.sep(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.okt(år: Int) = DagTidspunkt.med(LocalDate.of(år, 5, this))
fun Int.nov(år: Int) = DagTidspunkt.med(LocalDate.of(år, 11, this))
fun Int.des(år: Int) = DagTidspunkt.med(LocalDate.of(år, 12, this))

fun <T> TidspunktClosedRange<Måned>.med(t: T) = Periode(this, t)
