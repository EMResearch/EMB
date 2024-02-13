package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import java.time.LocalDate

data class BehandlingPåVentDto(
    val venteårsak: Venteårsak,
    val tidsfrist: LocalDate,
)
