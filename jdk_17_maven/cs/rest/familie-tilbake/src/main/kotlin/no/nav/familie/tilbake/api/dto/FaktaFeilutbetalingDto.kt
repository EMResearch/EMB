package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import java.math.BigDecimal
import java.time.LocalDate

data class FaktaFeilutbetalingDto(
    val varsletBeløp: Long? = null,
    val totalFeilutbetaltPeriode: Datoperiode,
    val feilutbetaltePerioder: List<FeilutbetalingsperiodeDto>,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val revurderingsvedtaksdato: LocalDate,
    val begrunnelse: String,
    val faktainfo: Faktainfo,
) {
    val gjelderDødsfall get() = feilutbetaltePerioder.any { it.hendelsestype == Hendelsestype.DØDSFALL }
}

data class FeilutbetalingsperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val hendelsestype: Hendelsestype? = null,
    val hendelsesundertype: Hendelsesundertype? = null,
)
