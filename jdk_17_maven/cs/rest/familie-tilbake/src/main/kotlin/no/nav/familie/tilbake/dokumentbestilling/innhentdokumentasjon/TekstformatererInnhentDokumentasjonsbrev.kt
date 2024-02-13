package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument

internal object TekstformatererInnhentDokumentasjonsbrev {

    fun lagFritekst(dokument: InnhentDokumentasjonsbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(dokument, "innhentdokumentasjon/innhent_dokumentasjon")
    }

    fun lagOverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(brevmetadata, "innhentdokumentasjon/innhent_dokumentasjon_overskrift")
    }
}
