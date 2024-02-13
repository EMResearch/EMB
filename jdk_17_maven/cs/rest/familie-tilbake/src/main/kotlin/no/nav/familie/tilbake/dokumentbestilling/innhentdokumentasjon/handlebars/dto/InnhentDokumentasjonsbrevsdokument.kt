package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class InnhentDokumentasjonsbrevsdokument(
    val brevmetadata: Brevmetadata,
    val fritekstFraSaksbehandler: String,
    val fristdato: LocalDate,
) : BaseDokument(
    brevmetadata.ytelsestype,
    brevmetadata.språkkode,
    brevmetadata.behandlendeEnhetsNavn,
    brevmetadata.ansvarligSaksbehandler,
    brevmetadata.gjelderDødsfall,
    brevmetadata.institusjon,
) {

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = BrevmottagerUtil.getAnnenMottagersNavn(brevmetadata)

    @Suppress("unused") // Handlebars
    val isRentepliktig = ytelsestype != Ytelsestype.BARNETRYGD && ytelsestype != Ytelsestype.KONTANTSTØTTE

    @Suppress("unused") // Handlebars
    val isBarnetrygd = ytelsestype == Ytelsestype.BARNETRYGD

    @Suppress("unused") // Handlebars
    val isKontantstøtte = ytelsestype == Ytelsestype.KONTANTSTØTTE

    init {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }
}
