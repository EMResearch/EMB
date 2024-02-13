package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import java.util.UUID

data class OpprettRevurderingDto(
    val ytelsestype: Ytelsestype, // kun brukes for tilgangskontroll
    val originalBehandlingId: UUID,
    val årsakstype: Behandlingsårsakstype,
)
