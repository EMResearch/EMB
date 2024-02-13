package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg

data class TotrinnsvurderingDto(val totrinnsstegsinfo: List<Totrinnsstegsinfo>)

data class Totrinnsstegsinfo(
    val behandlingssteg: Behandlingssteg,
    val godkjent: Boolean? = null,
    val begrunnelse: String? = null,
)
