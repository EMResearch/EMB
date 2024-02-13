package no.nav.familie.tilbake.dokumentbestilling.handlebars.dto

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata

data class Brevoverskriftsdata(val brevmetadata: Brevmetadata) : BaseDokument(
    brevmetadata.ytelsestype,
    brevmetadata.språkkode,
    brevmetadata.behandlendeEnhetsNavn,
    brevmetadata.ansvarligSaksbehandler,
    brevmetadata.gjelderDødsfall,
    brevmetadata.institusjon,
)
