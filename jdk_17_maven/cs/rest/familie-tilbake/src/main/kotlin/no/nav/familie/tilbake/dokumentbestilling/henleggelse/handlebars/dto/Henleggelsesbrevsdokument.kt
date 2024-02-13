package no.nav.familie.tilbake.dokumentbestilling.henleggelse.handlebars.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class Henleggelsesbrevsdokument(
    val brevmetadata: Brevmetadata,
    val varsletDato: LocalDate?,
    val fritekstFraSaksbehandler: String?,
) : BaseDokument(
    brevmetadata.ytelsestype,
    brevmetadata.språkkode,
    brevmetadata.behandlendeEnhetsNavn,
    brevmetadata.ansvarligSaksbehandler,
    brevmetadata.gjelderDødsfall,
    brevmetadata.institusjon,
) {

    private val tilbakekrevingsrevurdering = Behandlingstype.REVURDERING_TILBAKEKREVING == brevmetadata.behandlingstype

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = BrevmottagerUtil.getAnnenMottagersNavn(brevmetadata)

    init {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }

    fun init() {
        if (tilbakekrevingsrevurdering) {
            requireNotNull(fritekstFraSaksbehandler) { "fritekst kan ikke være null" }
        } else {
            requireNotNull(varsletDato) { "varsletDato kan ikke være null" }
        }
        if (finnesVerge) {
            requireNotNull(annenMottagersNavn) { "annenMottagersNavn kan ikke være null" }
        }
    }
}
