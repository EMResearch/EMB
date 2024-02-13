package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import java.math.BigDecimal

data class HbTotalresultat(
    val hovedresultat: Vedtaksresultat,
    val totaltTilbakekrevesBeløp: BigDecimal,
    val totaltTilbakekrevesBeløpMedRenter: BigDecimal,
    val totaltTilbakekrevesBeløpMedRenterUtenSkatt: BigDecimal,
    val totaltRentebeløp: BigDecimal,
) {

    val harSkattetrekk = totaltTilbakekrevesBeløpMedRenterUtenSkatt < totaltTilbakekrevesBeløpMedRenter
}
