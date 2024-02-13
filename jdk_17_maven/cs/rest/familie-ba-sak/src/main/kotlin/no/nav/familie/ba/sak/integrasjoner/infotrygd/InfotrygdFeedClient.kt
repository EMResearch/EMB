package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.StartBehandlingDto
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.io.IOException
import java.net.URI

@Component
class InfotrygdFeedClient(
    @Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val clientUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "infotrygd_feed") {

    fun sendFødselhendelsesFeedTilInfotrygd(infotrygdFødselhendelsesFeedDto: InfotrygdFødselhendelsesFeedDto) {
        return try {
            sendFeedTilInfotrygd(
                URI.create("$clientUri/barnetrygd/v1/feed/foedselsmelding"),
                infotrygdFødselhendelsesFeedDto,
            )
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    fun sendVedtakFeedTilInfotrygd(infotrygdVedtakFeedDto: InfotrygdVedtakFeedDto) {
        try {
            sendFeedTilInfotrygd(URI.create("$clientUri/barnetrygd/v1/feed/vedtaksmelding"), infotrygdVedtakFeedDto)
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    fun sendStartBehandlingTilInfotrygd(startBehandlingDto: StartBehandlingDto) {
        try {
            sendFeedTilInfotrygd(
                URI.create("$clientUri/barnetrygd/v1/feed/startbehandlingsmelding"),
                startBehandlingDto,
            )
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    private fun loggOgKastException(e: Exception) {
        if (e is HttpClientErrorException) {
            logger.warn("Http feil mot infotrygd feed: httpkode: ${e.statusCode}, feilmelding ${e.message}", e)
        } else {
            logger.warn("Feil mot infotrygd feed; melding ${e.message}", e)
        }

        throw e
    }

    @Retryable(
        value = [IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    private fun sendFeedTilInfotrygd(endpoint: URI, feed: Any) {
        postForEntity<Ressurs<String>>(endpoint, feed)
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(InfotrygdFeedClient::class.java)
    }
}
