package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlIdenter
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.Tema
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class PdlIdentRestClient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "pdl.ident") {
    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    private val hentIdenterQuery = hentGraphqlQuery("hentIdenter")

    @Cacheable("identer", cacheManager = "shortCache")
    fun hentIdenter(personIdent: String, historikk: Boolean): List<IdentInformasjon> {
        val pdlIdenter = hentIdenter(personIdent)

        return if (historikk) {
            pdlIdenter.identer.map { it }
        } else {
            pdlIdenter.identer.filter { !it.historisk }.map { it }
        }
    }

    private fun hentIdenter(personIdent: String): PdlIdenter {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentIdenterQuery,
        )
        val pdlResponse: PdlBaseResponse<PdlHentIdenterResponse> = kallEksternTjeneste(
            tjeneste = "pdl",
            uri = pdlUri,
            formål = "Hent identer",
        ) {
            postForEntity(
                pdlUri,
                pdlPersonRequest,
                httpHeaders(),
            )
        }

        return feilsjekkOgReturnerData(
            ident = personIdent,
            pdlResponse = pdlResponse,
        ) {
            it.pdlIdenter
        }
    }

    fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Tema", PDL_TEMA)
            add("behandlingsnummer", Tema.BAR.behandlingsnummer)
        }
    }

    companion object {

        private const val PATH_GRAPHQL = "graphql"
        private const val PDL_TEMA = "BAR"
    }
}
