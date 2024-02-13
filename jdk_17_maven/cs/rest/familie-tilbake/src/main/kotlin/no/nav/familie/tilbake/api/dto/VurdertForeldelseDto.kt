package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import java.math.BigDecimal
import java.time.LocalDate

data class VurdertForeldelseDto(val foreldetPerioder: List<VurdertForeldelsesperiodeDto>)

data class VurdertForeldelsesperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val begrunnelse: String? = null,
    val foreldelsesvurderingstype: Foreldelsesvurderingstype? = null,
    val foreldelsesfrist: LocalDate? = null,
    val oppdagelsesdato: LocalDate? = null,
)
