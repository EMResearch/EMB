package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbResultat(
    val tilbakekrevesBeløp: BigDecimal,
    val rentebeløp: BigDecimal,
    val foreldetBeløp: BigDecimal? = null,
    val tilbakekrevesBeløpUtenSkattMedRenter: BigDecimal,
) {

    val tilbakekrevesBeløpMedRenter: BigDecimal = tilbakekrevesBeløp.add(rentebeløp)
}
