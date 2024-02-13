package no.nav.familie.tilbake.api.dto

import jakarta.validation.constraints.Size
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import java.util.UUID

class BestillBrevDto(
    val behandlingId: UUID,
    val brevmalkode: Dokumentmalstype,
    @Size(min = 1, max = 3000)
    val fritekst: String,
)
