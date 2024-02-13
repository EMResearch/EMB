package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.AbstractMockkSpringRunner
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate

@SpringBootTest(
    classes = [ApplicationConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "no.nav.security.jwt.issuer.azuread.discoveryUrl: http://localhost:\${mock-oauth2-server.port}/azuread/.well-known/openid-configuration",
        "no.nav.security.jwt.issuer.azuread.accepted_audience: some-audience",
        "rolle.veileder: VEILDER",
        "rolle.saksbehandler: SAKSBEHANDLER",
        "rolle.beslutter: BESLUTTER",
    ],
)
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
abstract class WebSpringAuthTestRunner : AbstractMockkSpringRunner() {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @LocalServerPort
    private val port = 0

    fun hentUrl(path: String) = "http://localhost:$port$path"

    fun token(
        claims: Map<String, Any>,
        subject: String = DEFAULT_SUBJECT,
        audience: String = DEFAULT_AUDIENCE,
        issuerId: String = DEFAULT_ISSUER_ID,
        clientId: String = DEFAULT_CLIENT_ID,
    ): String {
        return mockOAuth2Server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = subject,
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
    }

    fun hentHeaders(groups: List<String>? = null): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.setBearerAuth(
            token(
                mapOf(
                    "groups" to (groups ?: listOf(BehandlerRolle.SAKSBEHANDLER.name)),
                    "azp" to "azp-test",
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        return httpHeaders
    }

    fun hentHeadersForSystembruker(groups: List<String>? = null): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.setBearerAuth(
            token(
                mapOf(
                    "groups" to (groups ?: listOf(BehandlerRolle.SYSTEM.name)),
                    "azp" to "azp-test",
                    "name" to SYSTEM_FORKORTELSE,
                    "preferred_username" to SYSTEM_FORKORTELSE,
                ),
            ),
        )
        return httpHeaders
    }

    companion object {

        const val DEFAULT_ISSUER_ID = "azuread"
        const val DEFAULT_SUBJECT = "subject"
        const val DEFAULT_AUDIENCE = "some-audience"
        const val DEFAULT_CLIENT_ID = "theclientid"
    }
}
