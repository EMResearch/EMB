package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.YearMonth
import java.util.SortedMap

data class LogiskPeriode(
    val periode: Månedsperiode,
    val feilutbetaltBeløp: BigDecimal,
) {

    val fom get() = periode.fom
    val tom get() = periode.tom
}

object LogiskPeriodeUtil {

    fun utledLogiskPeriode(feilutbetalingPrPeriode: SortedMap<Månedsperiode, BigDecimal>): List<LogiskPeriode> {
        var førsteMåned: YearMonth? = null
        var sisteMåned: YearMonth? = null
        var logiskPeriodeBeløp = BigDecimal.ZERO
        val resultat = mutableListOf<LogiskPeriode>()
        for ((periode, feilutbetaltBeløp) in feilutbetalingPrPeriode) {
            if (førsteMåned == null && sisteMåned == null) {
                førsteMåned = periode.fom
                sisteMåned = periode.tom
            } else {
                if (harOppholdMellom(sisteMåned!!, periode.fom)) {
                    resultat.add(
                        LogiskPeriode(
                            periode = Månedsperiode(førsteMåned!!, sisteMåned),
                            feilutbetaltBeløp = logiskPeriodeBeløp,
                        ),
                    )
                    førsteMåned = periode.fom
                    logiskPeriodeBeløp = BigDecimal.ZERO
                }
                sisteMåned = periode.tom
            }
            logiskPeriodeBeløp = logiskPeriodeBeløp.add(feilutbetaltBeløp)
        }
        if (BigDecimal.ZERO.compareTo(logiskPeriodeBeløp) != 0) {
            resultat.add(
                LogiskPeriode(
                    periode = Månedsperiode(førsteMåned!!, sisteMåned!!),
                    feilutbetaltBeløp = logiskPeriodeBeløp,
                ),
            )
        }
        return resultat.toList()
    }

    private fun harOppholdMellom(måned1: YearMonth, måned2: YearMonth): Boolean {
        require(måned2 > måned1) { "dag2 må være etter dag1" }
        return måned1.plusMonths(1) != måned2
    }
}
