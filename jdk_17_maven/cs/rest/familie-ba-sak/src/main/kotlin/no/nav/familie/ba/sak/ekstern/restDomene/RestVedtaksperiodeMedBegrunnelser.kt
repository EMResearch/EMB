package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class RestPutVedtaksperiodeMedFritekster(
    val fritekster: List<String> = emptyList(),
)

data class RestPutVedtaksperiodeMedStandardbegrunnelser(
    val standardbegrunnelser: List<String>,
)

data class RestGenererVedtaksperioderForOverstyrtEndringstidspunkt(
    val behandlingId: Long,
    val overstyrtEndringstidspunkt: LocalDate,
)

data class RestPutGenererFortsattInnvilgetVedtaksperioder(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long,
)
