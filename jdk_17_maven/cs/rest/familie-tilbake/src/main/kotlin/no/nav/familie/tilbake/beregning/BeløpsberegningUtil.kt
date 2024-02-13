package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.RoundingMode

object BeløpsberegningUtil {

    fun beregnBeløpPerMåned(beløp: BigDecimal, kravgrunnlagsperiode: Månedsperiode): BigDecimal {
        return beløp.divide(BigDecimal.valueOf(kravgrunnlagsperiode.lengdeIHeleMåneder()), 2, RoundingMode.HALF_UP)
    }

    fun beregnBeløp(vurderingsperiode: Månedsperiode, kravgrunnlagsperiode: Månedsperiode, beløpPerMåned: BigDecimal): BigDecimal {
        val overlapp = kravgrunnlagsperiode.snitt(vurderingsperiode)
        if (overlapp != null) {
            return beløpPerMåned.multiply(BigDecimal.valueOf(overlapp.lengdeIHeleMåneder()))
        }
        return BigDecimal.ZERO
    }

    fun beregnBeløpForPeriode(
        tilbakekrevesBeløp: BigDecimal,
        vurderingsperiode: Månedsperiode,
        kravgrunnlagsperiode: Månedsperiode,
    ): BigDecimal {
        val grunnlagBeløpPerMåned: BigDecimal = beregnBeløpPerMåned(tilbakekrevesBeløp, kravgrunnlagsperiode)
        val ytelseBeløp: BigDecimal = beregnBeløp(vurderingsperiode, kravgrunnlagsperiode, grunnlagBeløpPerMåned)
        return ytelseBeløp.setScale(0, RoundingMode.HALF_UP)
    }
}
