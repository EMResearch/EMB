package no.nav.familie.ba.sak.sikkerhet

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.postForEntity

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "mock-pdl",
    "mock-ident-client",
    "mock-infotrygd-barnetrygd",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-økonomi",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service",
)
class RolletilgangTest(
    @Autowired
    private val fagsakService: FagsakService,
) : WebSpringAuthTestRunner() {

    @Test
    fun `Skal kaste feil når innlogget veileder prøver å opprette fagsak gjennom rest-endepunkt`() {
        val fnr = randomFnr()

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity = HttpEntity<String>(
            objectMapper.writeValueAsString(
                FagsakRequest(
                    personIdent = fnr,
                ),
            ),
            header,
        )

        val error = assertThrows<HttpClientErrorException> {
            restTemplate.postForEntity<Ressurs<Fagsak>>(
                hentUrl("/api/fagsaker"),
                requestEntity,
            )
        }

        val ressurs: Ressurs<Fagsak> = objectMapper.readValue(error.responseBodyAsString)

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
        assertEquals(Ressurs.Status.IKKE_TILGANG, ressurs.status)
        assertEquals(
            "Mock McMockface med rolle VEILEDER har ikke tilgang til å opprette fagsak. Krever SAKSBEHANDLER.",
            ressurs.melding,
        )
    }

    @Test
    fun `Skal få 201 når innlogget saksbehandler prøver å opprette fagsak gjennom rest-endepunkt, tester også db-tilgangskontroll`() {
        val fnr = randomFnr()

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER", "SAKSBEHANDLER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity = HttpEntity<String>(
            objectMapper.writeValueAsString(
                FagsakRequest(
                    personIdent = fnr,
                ),
            ),
            header,
        )

        val response = restTemplate.postForEntity<Ressurs<Fagsak>>(hentUrl("/api/fagsaker"), requestEntity)
        val ressurs = response.body

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(Ressurs.Status.SUKSESS, ressurs?.status)
    }

    @Test
    fun `Skal kaste feil når innlogget veileder prøver å opprette behandling gjennom test-rest-endepunkt som validerer på db-nivå`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity = HttpEntity<String>(
            objectMapper.writeValueAsString(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.data!!.id)),
            header,
        )

        val error = assertThrows<HttpClientErrorException> {
            restTemplate.postForEntity<Ressurs<Behandling>>(
                hentUrl("/rolletilgang/test-behandlinger"),
                requestEntity,
            )
        }

        val ressurs: Ressurs<Behandling> = objectMapper.readValue(error.responseBodyAsString)

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
        assertEquals(Ressurs.Status.IKKE_TILGANG, ressurs.status)
        assertEquals(
            "Mock McMockface med rolle VEILEDER har ikke skrivetilgang til databasen.",
            ressurs.melding,
        )
    }
}
