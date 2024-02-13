package no.nav.familie.tilbake.beregning.modell

import java.math.BigDecimal

class Beregningsresultat(
    val beregningsresultatsperioder: List<Beregningsresultatsperiode>,
    val vedtaksresultat: Vedtaksresultat,
) {

    val totaltTilbakekrevesUtenRenter = beregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløpUtenRenter }
    val totaltTilbakekrevesMedRenter = beregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløp }
    val totaltRentebeløp = beregningsresultatsperioder.sumOf { it.rentebeløp }
    private val totaltSkattetrekk = beregningsresultatsperioder.sumOf { it.skattebeløp }
    val totaltTilbakekrevesBeløpMedRenterUtenSkatt: BigDecimal = totaltTilbakekrevesMedRenter.subtract(totaltSkattetrekk)
    val totaltFeilutbetaltBeløp = beregningsresultatsperioder.sumOf { it.feilutbetaltBeløp }
}
