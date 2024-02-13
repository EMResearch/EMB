package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import java.time.LocalDate

class HbBehandling(
    val erRevurdering: Boolean = false,
    val erRevurderingEtterKlageNfp: Boolean = false,
    val originalBehandlingsdatoFagsakvedtak: LocalDate? = null,
    val erRevurderingEtterKlage: Boolean = false,
) {

    init {
        if (erRevurdering) {
            requireNotNull(originalBehandlingsdatoFagsakvedtak) { "vedtaksdato for original behandling er ikke satt" }
        } else {
            require(!erRevurderingEtterKlageNfp) { "En revurdering etter klage må være en revurdering." }
        }
    }
}
