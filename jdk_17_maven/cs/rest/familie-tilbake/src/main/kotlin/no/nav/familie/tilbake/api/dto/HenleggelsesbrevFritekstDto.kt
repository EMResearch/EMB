package no.nav.familie.tilbake.api.dto

import jakarta.validation.constraints.Size
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype

data class HenleggelsesbrevFritekstDto(
    val behandlingsresultatstype: Behandlingsresultatstype,
    val begrunnelse: String,
    @Size(max = 1500, message = "Fritekst er for lang")
    val fritekst: String? = null,
)
