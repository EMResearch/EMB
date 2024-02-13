package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.MockserverKlient
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.web.client.RestOperations
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.images.PullPolicy

val MOCK_SERVER_IMAGE = "ghcr.io/navikt/familie-mock-server/familie-mock-server:latest"

class VerdikjedetesterPropertyOverrideContextInitializer :
    ApplicationContextInitializer<ConfigurableApplicationContext?> {

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "FAMILIE_BA_INFOTRYGD_API_URL: http://localhost:1337/rest/api/infotrygd/ba",
            "PDL_URL: http://localhost:1337/rest/api/pdl",
        )
        mockServer.start()
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        val mockServer: KMockServerContainer by lazy {
            val mockServer = KMockServerContainer(MOCK_SERVER_IMAGE)
            mockServer.withExposedPorts(1337)
            mockServer.withFixedExposedPort(1337, 1337)
            mockServer.withImagePullPolicy(PullPolicy.alwaysPull())
            mockServer
        }
    }
}

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "mock-oauth",
    "mock-localdate-service",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-økonomi",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service",
    "mock-sanity-client",
)
@ContextConfiguration(initializers = [VerdikjedetesterPropertyOverrideContextInitializer::class])
@Tag("verdikjedetest")
abstract class AbstractVerdikjedetest : WebSpringAuthTestRunner() {

    @Autowired
    lateinit var restOperations: RestOperations

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
        baSakUrl = hentUrl(""),
        restOperations = restOperations,
        headers = hentHeadersForSystembruker(),
    )

    fun mockServerKlient(): MockserverKlient = MockserverKlient(
        mockServerUrl = "http://localhost:1337",
        restOperations = restOperations,
    )
}

/**
 * Hack needed because testcontainers use of generics confuses Kotlin.
 * Må bruke fixed host port for at klientene våres kan konfigureres med fast port.
 */
class KMockServerContainer(imageName: String) : FixedHostPortGenericContainer<KMockServerContainer>(imageName)
