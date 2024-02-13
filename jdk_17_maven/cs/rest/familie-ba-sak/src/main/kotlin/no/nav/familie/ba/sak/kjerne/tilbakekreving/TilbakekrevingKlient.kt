package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

typealias TilbakekrevingId = String

data class FinnesBehandlingsresponsDto(val finnesÅpenBehandling: Boolean)

@Component
class TilbakekrevingKlient(
    @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "Tilbakekreving") {

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val uri = URI.create("$familieTilbakeUri/dokument/forhandsvis-varselbrev")

        return kallEksternTjeneste(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter forhåndsvisning av varselbrev",
        ) {
            postForEntity(
                uri = uri,
                payload = forhåndsvisVarselbrevRequest,
                httpHeaders = HttpHeaders().apply {
                    accept = listOf(MediaType.APPLICATION_PDF)
                },
            )
        }
    }

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): TilbakekrevingId {
        val uri = URI.create("$familieTilbakeUri/behandling/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter behandling for tilbakekreving",
        ) {
            postForEntity(uri, opprettTilbakekrevingRequest)
        }
    }

    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/$fagsakId/finnesApenBehandling/v1")

        val finnesBehandlingsresponsDto: FinnesBehandlingsresponsDto = kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Sjekker om en fagsak har åpen tilbakekrevingsbehandling",
        ) { getForEntity(uri) }

        return finnesBehandlingsresponsDto.finnesÅpenBehandling
    }

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/$fagsakId/behandlinger/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsbehandlinger på fagsak",
        ) { getForEntity(uri) }
    }

    fun hentTilbakekrevingsvedtak(fagsakId: Long): List<FagsystemVedtak> {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/$fagsakId/vedtak/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsvedtak på fagsak",
        ) { getForEntity(uri) }
    }

    fun kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId: Long): KanBehandlingOpprettesManueltRespons {
        val uri = URI.create(
            "$familieTilbakeUri/ytelsestype/${Ytelsestype.BARNETRYGD}/fagsak/$fagsakId/kanBehandlingOpprettesManuelt/v1",
        )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Sjekker om tilbakekrevingsbehandling kan opprettes manuelt",
        ) { getForEntity(uri) }
    }

    fun opprettTilbakekrevingsbehandlingManuelt(request: OpprettManueltTilbakekrevingRequest): String {
        val uri = URI.create("$familieTilbakeUri/behandling/manuelt/task/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter tilbakekrevingsbehandling manuelt",
        ) { postForEntity(uri, request) }
    }
}
