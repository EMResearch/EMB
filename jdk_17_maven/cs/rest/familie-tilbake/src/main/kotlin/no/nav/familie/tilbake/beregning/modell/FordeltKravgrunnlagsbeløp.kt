package no.nav.familie.tilbake.beregning.modell

import java.math.BigDecimal

class FordeltKravgrunnlagsbeløp(
    val feilutbetaltBeløp: BigDecimal,
    val utbetaltYtelsesbeløp: BigDecimal,
    val riktigYtelsesbeløp: BigDecimal,
)
