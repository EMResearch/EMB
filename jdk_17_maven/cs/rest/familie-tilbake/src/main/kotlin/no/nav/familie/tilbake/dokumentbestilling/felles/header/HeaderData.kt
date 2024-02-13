package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte

class HeaderData(
    override val språkkode: Språkkode,
    val person: Person,
    val brev: Brev,
    val institusjon: Institusjon? = null,
) : Språkstøtte
