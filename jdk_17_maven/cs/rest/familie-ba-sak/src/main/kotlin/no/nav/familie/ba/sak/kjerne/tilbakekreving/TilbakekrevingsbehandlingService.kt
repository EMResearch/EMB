package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.RestTilbakekrevingsbehandling
import org.springframework.stereotype.Service

@Service
class TilbakekrevingsbehandlingService(private val tilbakekrevingKlient: TilbakekrevingKlient) {

    fun hentRestTilbakekrevingsbehandlinger(fagsakId: Long): List<RestTilbakekrevingsbehandling> {
        val behandlinger = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsakId)
        return behandlinger.map {
            RestTilbakekrevingsbehandling(
                behandlingId = it.behandlingId,
                opprettetTidspunkt = it.opprettetTidspunkt,
                aktiv = it.aktiv,
                årsak = it.årsak,
                type = it.type,
                status = it.status,
                resultat = it.resultat,
                vedtaksdato = it.vedtaksdato,
            )
        }
    }
}
