package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Brevoverskriftsdata
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.handlebars.dto.Henleggelsesbrevsdokument

internal object TekstformatererHenleggelsesbrev {

    fun lagFritekst(dokument: Henleggelsesbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(dokument, "henleggelse/henleggelse")
    }

    fun lagOverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), "henleggelse/henleggelse_overskrift")
    }

    fun lagRevurderingsfritekst(dokument: Henleggelsesbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(dokument, "henleggelse/henleggelse_revurdering")
    }

    fun lagRevurderingsoverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(
            Brevoverskriftsdata(brevmetadata),
            "henleggelse/henleggelse_revurdering_overskrift",
        )
    }
}
