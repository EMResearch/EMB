package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import java.math.BigDecimal

data class Beregningsresultatsperiode(
    val periode: Månedsperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevingsbeløpUtenRenter: BigDecimal,
    val rentebeløp: BigDecimal,
    val tilbakekrevingsbeløp: BigDecimal,
    val skattebeløp: BigDecimal,
    val tilbakekrevingsbeløpEtterSkatt: BigDecimal,
    val utbetaltYtelsesbeløp: BigDecimal, // Rått beløp, ikke justert for ev. trekk
    val riktigYtelsesbeløp: BigDecimal,
) // Rått beløp, ikke justert for ev. trekk
