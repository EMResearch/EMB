package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import java.math.BigDecimal

data class BeregningsresultatDto(
    val beregningsresultatsperioder: List<BeregningsresultatsperiodeDto>,
    val vedtaksresultat: Vedtaksresultat,
)

data class BeregningsresultatsperiodeDto(
    val periode: Datoperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val tilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevesBeløpEtterSkatt: BigDecimal? = null,
)
