package no.nav.familie.tilbake.api.dto

import jakarta.validation.constraints.Size
import java.util.UUID

data class ForhåndsvisningHenleggelsesbrevDto(
    val behandlingId: UUID,
    @Size(max = 1500, message = "Fritekst er for lang")
    val fritekst: String?,
)
