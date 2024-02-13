package no.nav.familie.ba.sak.integrasjoner.statistikk

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestOperations
import java.net.URI

internal class StatistikkClientTest : AbstractSpringIntegrationTest() {
    lateinit var client: StatistikkClient

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @BeforeEach
    fun setUp() {
        client = StatistikkClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations,
        )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    fun harSendtVedtaksmeldingForBehandling() {
        wireMockServer.stubFor(
            WireMock.get("/api/vedtak/123").willReturn(
                WireMock.okJson(
                    objectMapper.writeValueAsString(
                        Ressurs.success(true),
                    ),
                ),
            ),
        )

        assertEquals(
            client.harSendtVedtaksmeldingForBehandling(123),
            true,
        )
    }
}
