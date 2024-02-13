package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager

data class Brevdata(
    var metadata: Brevmetadata,
    val tittel: String? = null,
    val overskrift: String,
    val mottager: Brevmottager,
    val brevtekst: String,
    val vedleggHtml: String = "",
)
