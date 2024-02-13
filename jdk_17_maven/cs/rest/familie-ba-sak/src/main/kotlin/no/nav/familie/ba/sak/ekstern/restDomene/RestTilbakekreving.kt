package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg

class RestTilbakekreving(
    val valg: Tilbakekrevingsvalg,
    val varsel: String? = null,
    val begrunnelse: String,
    val tilbakekrevingsbehandlingId: String? = null,
)
