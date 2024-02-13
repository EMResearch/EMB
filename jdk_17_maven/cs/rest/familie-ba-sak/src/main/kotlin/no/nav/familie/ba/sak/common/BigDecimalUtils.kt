package no.nav.familie.ba.sak.common

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object BigDecimalKonstanter {
    val AVRUND_OPP: RoundingMode = RoundingMode.HALF_UP
}

fun BigDecimal.del(divident: BigDecimal, scale: Int) = this.divide(divident, scale, BigDecimalKonstanter.AVRUND_OPP)

fun BigDecimal.multipliser(multiplikator: BigDecimal, precision: Int) = this.multiply(multiplikator, MathContext(precision, BigDecimalKonstanter.AVRUND_OPP))
