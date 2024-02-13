package no.nav.familie.tilbake.integration.pdl

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.PdlAdressebeskyttelsePerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlBolkResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentPersonResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlPerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonBolkRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonBolkRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.integration.pdl.internal.feilsjekkOgReturnerData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class PdlClient(
    private val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") restTemplate: RestOperations,
) :
    AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun hentPersoninfo(ident: String, fagsystem: Fagsystem): Personinfo {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(ident),
            query = PdlConfig.hentEnkelPersonQuery,
        )
        val respons: PdlHentPersonResponse<PdlPerson> = postForEntity(
            pdlConfig.pdlUri,
            pdlPersonRequest,
            httpHeaders(mapTilTema(fagsystem)),
        )
        if (respons.harAdvarsel()) {
            logger.warn("Advarsel ved henting av personinfo fra PDL. Se securelogs for detaljer.")
            secureLogger.warn("Advarsel ved henting av personinfo fra PDL: ${respons.extensions?.warnings}")
        }
        if (!respons.harFeil()) {
            return respons.data.person!!.let {
                val aktivtIdent = it.identer.first().identifikasjonsnummer ?: error("Kan ikke hente aktivt ident fra PDL")
                Personinfo(
                    ident = aktivtIdent,
                    fødselsdato = LocalDate.parse(it.fødsel.first().fødselsdato!!),
                    navn = it.navn.first().fulltNavn(),
                    kjønn = it.kjønn.first().kjønn,
                    dødsdato = it.dødsfall.firstOrNull()?.let { dødsfall -> LocalDate.parse(dødsfall.dødsdato) },
                )
            }
        } else {
            logger.warn("Respons fra PDL:${objectMapper.writeValueAsString(respons)}")
            throw Feil(
                message = "Feil ved oppslag på person: ${respons.errorMessages()}",
                frontendFeilmelding = "Feil ved oppslag på person $ident: ${respons.errorMessages()}",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
    }

    fun hentIdenter(personIdent: String, fagsystem: Fagsystem): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = PdlConfig.hentIdenterQuery,
        )
        val response = postForEntity<PdlHentIdenterResponse>(
            pdlConfig.pdlUri,
            pdlPersonRequest,
            httpHeaders(mapTilTema(fagsystem)),
        )
        if (response.harAdvarsel()) {
            logger.warn("Advarsel ved henting av personidenter fra PDL. Se securelogs for detaljer.")
            secureLogger.warn("Advarsel ved henting av personidenter fra PDL: ${response.extensions?.warnings}")
        }
        if (!response.harFeil()) return response
        throw Feil(
            message = "Feil mot pdl: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND,
        )
    }

    fun hentAdressebeskyttelseBolk(personIdentList: List<String>, fagsystem: Fagsystem): Map<String, PdlAdressebeskyttelsePerson> {
        val pdlRequest = PdlPersonBolkRequest(
            variables = PdlPersonBolkRequestVariables(personIdentList),
            query = PdlConfig.hentAdressebeskyttelseBolkQuery,
        )
        val pdlResponse = postForEntity<PdlBolkResponse<PdlAdressebeskyttelsePerson>>(
            pdlConfig.pdlUri,
            pdlRequest,
            httpHeaders(mapTilTema(fagsystem)),
        )
        return feilsjekkOgReturnerData(
            pdlResponse = pdlResponse,
        )
    }

    private fun httpHeaders(tema: Tema): HttpHeaders {
        return HttpHeaders().apply {
            add("Tema", tema.name)
            add("behandlingsnummer", tema.behandlingsnummer)
        }
    }

    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }
    private fun mapTilTema(fagsystem: Fagsystem): Tema {
        return when (fagsystem) {
            Fagsystem.EF -> Tema.ENF
            Fagsystem.KONT -> Tema.KON
            Fagsystem.BA -> Tema.BAR
            else -> error("Ugyldig fagsystem=${fagsystem.navn}")
        }
    }
}

/**
 * TODO : Fjern når versjon 3 av kontrakter blir tatt i bruk.
 */
private enum class Tema(val fagsaksystem: String, val behandlingsnummer: String) {
    BAR("BA", "B284"),
    ENF("EF", "B288"),
    KON("KONT", "B278"),
}
