package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

data class HentFagsystemsbehandlingRequestDto(
    val ytelsestype: Ytelsestype,
    val eksternFagsakId: String,
    val eksternId: String,
)
