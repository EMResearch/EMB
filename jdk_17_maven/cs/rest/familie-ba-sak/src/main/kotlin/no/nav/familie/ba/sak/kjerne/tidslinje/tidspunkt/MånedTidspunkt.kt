package no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt

import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

data class MånedTidspunkt internal constructor(
    internal val måned: YearMonth,
    override val uendelighet: Uendelighet,
) : Tidspunkt<Måned>(uendelighet) {

    fun tilYearMonthEllerNull(): YearMonth? =
        if (uendelighet != Uendelighet.INGEN) {
            null
        } else {
            måned
        }

    fun tilYearMonth(): YearMonth =
        if (uendelighet != Uendelighet.INGEN) {
            throw IllegalStateException("Tidspunktet er uendelig")
        } else {
            måned
        }

    override fun flytt(tidsenheter: Long) = copy(måned = måned.plusMonths(tidsenheter))

    override fun medUendelighet(uendelighet: Uendelighet): MånedTidspunkt =
        copy(uendelighet = uendelighet)

    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + måned + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }

    override fun sammenliknMed(tidspunkt: Tidspunkt<Måned>): Int {
        return måned.compareTo((tidspunkt as MånedTidspunkt).måned)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is MånedTidspunkt -> compareTo(other) == 0
            is Tidspunkt<*> -> this.uendelighet != Uendelighet.INGEN && this.uendelighet == other.uendelighet
            else -> false
        }
    }

    override fun hashCode(): Int {
        return if (uendelighet == Uendelighet.INGEN) {
            måned.hashCode()
        } else {
            uendelighet.hashCode()
        }
    }

    companion object {
        fun nå() = MånedTidspunkt(YearMonth.now(), Uendelighet.INGEN)
        fun uendeligLengeTil(måned: YearMonth = YearMonth.now()) = MånedTidspunkt(måned, Uendelighet.FREMTID)
        fun uendeligLengeSiden(måned: YearMonth = YearMonth.now()) = MånedTidspunkt(måned, Uendelighet.FORTID)
        fun med(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.INGEN)
        fun med(år: Int, måned: Int) = MånedTidspunkt(YearMonth.of(år, måned), Uendelighet.INGEN)

        internal fun YearMonth.tilTidspunkt() = MånedTidspunkt(this, Uendelighet.INGEN)

        internal fun YearMonth?.tilTidspunktEllerUendeligTidlig(defaultUendelighetMåned: YearMonth? = null) =
            this.tilTidspunktEllerUendelig(defaultUendelighetMåned, Uendelighet.FORTID)

        internal fun YearMonth?.tilTidspunktEllerUendeligSent(defaultUendelighetMåned: YearMonth? = null) =
            this.tilTidspunktEllerUendelig(defaultUendelighetMåned, Uendelighet.FREMTID)

        private fun YearMonth?.tilTidspunktEllerUendelig(default: YearMonth?, uendelighet: Uendelighet) =
            this?.let { MånedTidspunkt(it, Uendelighet.INGEN) } ?: MånedTidspunkt(
                default ?: YearMonth.now(),
                uendelighet,
            )

        fun LocalDate.tilMånedTidspunkt() = MånedTidspunkt(this.toYearMonth(), Uendelighet.INGEN)
    }
}
