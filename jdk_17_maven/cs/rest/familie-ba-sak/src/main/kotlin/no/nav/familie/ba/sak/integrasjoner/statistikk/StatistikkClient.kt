package no.nav.familie.ba.sak.integrasjoner.statistikk

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class StatistikkClient(
    @Value("\${FAMILIE_STATISTIKK_URL}") val baseUri: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "statistikk") {

    fun harSendtVedtaksmeldingForBehandling(behandlingId: Long): Boolean {
        val uri = URI.create("$baseUri/vedtak/$behandlingId")

        return try {
            val response: Ressurs<Boolean> = getForEntity(uri, httpHeaders())
            response.getDataOrThrow()
        } catch (e: Exception) {
            if (e is HttpStatusCodeException) {
                logger.error(
                    "Kall mot statistikk feilet: httpkode: ${e.statusCode}, body ${e.responseBodyAsString} ",
                    e,
                )
            }
            throw e
        }
    }

    private fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            add(HttpHeaders.CONTENT_TYPE, "application/json")
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(StatistikkClient::class.java)
    }
}
