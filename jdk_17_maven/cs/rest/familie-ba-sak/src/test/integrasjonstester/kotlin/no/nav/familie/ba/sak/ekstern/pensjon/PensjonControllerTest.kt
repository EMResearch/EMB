package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import java.util.Arrays
import java.util.UUID

@ActiveProfiles("postgres", "integrasjonstest", "mock-pdl", "mock-ident-client", "mock-oauth", "mock-brev-klient")
class PensjonControllerTest : WebSpringAuthTestRunner() {

    @Test
    fun `Verifiser at pensjon-endepunkt - bestillPersonerMedBarnetrygdForGittÅrPåKafka - for henting av identer med barnetrygd - returnerer en gyldig UUID som string`() {
        val headers = HttpHeaders()
        headers.accept = Arrays.asList(MediaType.TEXT_PLAIN)
        headers.setBearerAuth(
            hentTokenForPsys(),
        )
        val entity: HttpEntity<String> = HttpEntity<String>(headers)
        val responseEntity: ResponseEntity<String> = restTemplate.exchange(
            hentUrl("/api/ekstern/pensjon/bestill-personer-med-barnetrygd/2023"),
            HttpMethod.GET,
            entity,
            String::class.java,
        )
        assertEquals(UUID.fromString(responseEntity.body.toString()).toString(), responseEntity.body.toString())
    }

    private fun hentTokenForPsys() = token(
        mapOf(
            "groups" to listOf("SAKSBEHANDLER"),
            "name" to "Mock McMockface",
            "NAVident" to "Z0000",
        ),
        clientId = "omsorgsopptjening",
    )
}
