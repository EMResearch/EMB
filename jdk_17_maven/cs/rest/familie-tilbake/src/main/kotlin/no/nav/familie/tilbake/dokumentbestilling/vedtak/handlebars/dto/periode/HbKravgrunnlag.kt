package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbKravgrunnlag(
    val riktigBeløp: BigDecimal? = null,
    val utbetaltBeløp: BigDecimal? = null,
    val feilutbetaltBeløp: BigDecimal,
) {

    companion object {

        fun forFeilutbetaltBeløp(feilutbetaltBeløp: BigDecimal): HbKravgrunnlag {
            return HbKravgrunnlag(feilutbetaltBeløp = feilutbetaltBeløp)
        }
    }
}
