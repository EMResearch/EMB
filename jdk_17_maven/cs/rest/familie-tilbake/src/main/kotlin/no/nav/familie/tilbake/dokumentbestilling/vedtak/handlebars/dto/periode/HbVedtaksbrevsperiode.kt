package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import java.math.BigDecimal

data class HbVedtaksbrevsperiode(
    val periode: Datoperiode,
    val kravgrunnlag: HbKravgrunnlag,
    val fakta: HbFakta,
    val vurderinger: HbVurderinger,
    val resultat: HbResultat,
    val førstePeriode: Boolean,
    val grunnbeløp: HbGrunnbeløp? = null,
) {

    init {
        if (fakta.hendelsesundertype == Hendelsesundertype.INNTEKT_OVER_6G) {
            require(grunnbeløp != null) { "${Hendelsesundertype.INNTEKT_OVER_6G} krever verdi for grunnbeløp." }
        }
    }
}

data class HbGrunnbeløp(val grunnbeløpGanger6: BigDecimal?, val tekst6GangerGrunnbeløp: String?)
