package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.bisys.BisysUtvidetBarnetrygdResponse
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderRequest
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.YearMonth

@Component
class InfotrygdBarnetrygdClient(
    @Value("\${FAMILIE_BA_INFOTRYGD_API_URL}") private val clientUri: URI,
    @Qualifier("jwtBearerMedLangTimeout") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "infotrygd") {

    fun harLøpendeSakIInfotrygd(søkersIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/lopende-barnetrygd")

        val request = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        return try {
            postForEntity<InfotrygdLøpendeBarnetrygdResponse>(uri, request).harLøpendeBarnetrygd
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw ex
        }
    }

    fun harÅpenSakIInfotrygd(søkersIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/aapen-sak")

        val request = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        return try {
            postForEntity<InfotrygdÅpenSakResponse>(uri, request).harÅpenSak
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw ex
        }
    }

    fun hentSaker(søkersIdenter: List<String>, barnasIdenter: List<String> = emptyList()): InfotrygdSøkResponse<Sak> {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/saker")

        return try {
            postForEntity(uri, InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(
                message = "Henting av infotrygdsaker feilet. Gav feil: ${ex.message}",
                frontendFeilmelding = "Henting av infotrygdsaker feilet.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = ex,
            )
        }
    }

    fun hentStønader(
        søkersIdenter: List<String>,
        barnasIdenter: List<String>,
        historikk: Boolean = false,
    ): InfotrygdSøkResponse<Stønad> {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/stonad?historikk=$historikk")

        return try {
            postForEntity(uri, InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(
                message = "Henting av infotrygdstønader feilet. Gav feil: ${ex.message}",
                frontendFeilmelding = "Henting av infotrygdstønader feilet.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = ex,
            )
        }
    }

    data class HentUtvidetBarnetrygdRequest(val personIdent: String, val fraDato: YearMonth)

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: YearMonth): BisysUtvidetBarnetrygdResponse {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/utvidet")
        val body = HentUtvidetBarnetrygdRequest(personIdent, fraDato)
        return try {
            postForEntity(uri, body)
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av utvidet barnetrygd feilet. Gav feil: ${ex.message}", ex)
        }
    }

    fun hentPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/utvidet?aar=$år")
        return try {
            getForEntity(uri)
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av personer med utvidet barnetrygd feilet. Gav feil: ${ex.message}", ex)
        }
    }

    fun hentPerioderMedUtvidetBarnetrygdForPersoner(identer: List<String>, år: String): List<SkatteetatenPerioder> {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/utvidet/skatteetaten/perioder")

        val request = SkatteetatenPerioderRequest(identer = identer, aar = år)
        return try {
            postForEntity<List<SkatteetatenPerioderResponse>>(uri, request).flatMap { it.brukere }
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av perioder med utvidet barnetrygd feilet. Gav feil: ${ex.message}", ex)
        }
    }

    fun harNyligSendtBrevFor(søkersIdenter: List<String>, brevkoder: List<InfotrygdBrevkode>): SendtBrevResponse {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/brev")
        return try {
            postForEntity(
                uri,
                SendtBrevRequest(søkersIdenter, brevkoder.map { it.kode }),
            )
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException(
                "Sjekk mot infotrygd for å sjekke om brev er sendt feilet . Gav feil: ${ex.message}",
                ex,
            )
        }
    }

    class SendtBrevRequest(
        val personidenter: List<String>,
        val brevkoder: List<String>,
    )

    data class SendtBrevResponse(
        val harSendtBrev: Boolean,
        val listeBrevhendelser: List<InfotrygdHendelse> = emptyList(),
    )

    data class InfotrygdHendelse(
        val id: Long,
        val personKey: Long,
        val saksblokk: String,
        val saksnummer: String,
        val aksjonsdatoSeq: Long,
        val tekstKode1: String,
        val fnr: FoedselsNr,
        val tkNr: String,
        val region: String,
    )

    private fun loggFeil(ex: Exception, uri: URI) {
        when (ex) {
            is HttpClientErrorException -> secureLogger.warn(
                "Http feil mot ${uri.path}: httpkode: ${ex.statusCode}, feilmelding ${ex.message}",
                ex,
            )
            else -> secureLogger.warn("Feil mot ${uri.path}; melding ${ex.message}", ex)
        }
        logger.warn("Feil mot ${uri.path}.")
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(InfotrygdBarnetrygdClient::class.java)
    }
}

enum class InfotrygdBrevkode(val kode: String) {
    BREV_BATCH_OPPHØR_SMÅBARNSTILLLEGG("BA04"),
    BREV_BATCH_INNVILGET_SMÅBARNSTILLEGG("BA05"),
    BREV_BATCH_OMREGNING_BARN_18_ÅR("BA37"),
    BREV_BATCH_OMREGNING_BARN_6_ÅR("BA18"),
    BREV_MANUELL_OPPHØR_SMÅBARNSTILLLEGG("B001"),
    BREV_MANUELL_INNVILGET_SMÅBARNSTILLEGG("B002"),
    BREV_MANUELL_OMREGNING_BARN_18_ÅR("B003"),
    BREV_MANUELL_OMREGNING_BARN_6_ÅR("BA19"),
}
