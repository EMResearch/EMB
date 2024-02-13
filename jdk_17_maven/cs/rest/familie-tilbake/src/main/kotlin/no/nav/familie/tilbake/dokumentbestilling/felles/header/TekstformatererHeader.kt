package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.sanitize
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer

object TekstformatererHeader {

    fun lagHeader(brevmetadata: Brevmetadata, overskrift: String): String {
        return lagHeader(
            HeaderData(
                språkkode = brevmetadata.språkkode,
                person = Person(brevmetadata.sakspartsnavn, brevmetadata.sakspartId),
                brev = Brev(overskrift),
                institusjon = if (brevmetadata.institusjon != null) Institusjon(brevmetadata.institusjon.organisasjonsnummer, sanitize(brevmetadata.institusjon.navn)) else null,
            ),
        )
    }

    private fun lagHeader(data: HeaderData): String {
        return FellesTekstformaterer.lagBrevtekst(data, "header")
    }
}
