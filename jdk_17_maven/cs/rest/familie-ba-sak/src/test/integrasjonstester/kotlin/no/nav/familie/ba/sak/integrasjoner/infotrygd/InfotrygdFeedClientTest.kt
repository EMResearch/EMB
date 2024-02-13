package no.nav.familie.ba.sak.integrasjoner.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

class InfotrygdFeedClientTest : AbstractSpringIntegrationTest() {

    lateinit var client: InfotrygdFeedClient

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @BeforeEach
    fun setUp() {
        client = InfotrygdFeedClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations,
        )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    @Tag("integration")
    fun `skal legge til fødselsnummer i infotrygd feed`() {
        wireMockServer.stubFor(
            post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(
                okJson(objectMapper.writeValueAsString(success("Create"))),
            ),
        )
        val request = InfotrygdFødselhendelsesFeedTaskDto(listOf("fnr"))

        request.fnrBarn.forEach {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto(fnrBarn = it))
        }

        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        objectMapper.writeValueAsString(InfotrygdFødselhendelsesFeedDto(fnrBarn = request.fnrBarn.first())),
                    ),
                ),
        )
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd feed genererer http feil`() {
        wireMockServer.stubFor(post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(aResponse().withStatus(401)))

        assertThrows<HttpClientErrorException> {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto("fnr"))
        }
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd returnerer ulovlig response format`() {
        wireMockServer.stubFor(post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(aResponse().withBody("Create")))

        assertThrows<RuntimeException> {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto("fnr"))
        }
    }
}
