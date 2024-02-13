package no.nav.familie.tilbake.integration.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class PdlClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var pdlClient: PdlClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(wireMockConfig().dynamicPort())
            wiremockServerItem.start()

            pdlClient = PdlClient(PdlConfig(URI.create(wiremockServerItem.baseUrl())), restOperations)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `hentPersoninfo skal hente person info for barnetrygd med ok respons fra PDL`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdlOkResponseEnkel.json"))),
        )

        val respons = pdlClient.hentPersoninfo("11111122222", Fagsystem.BA)

        respons.shouldNotBeNull()
        respons.navn shouldBe "ENGASJERT FYR"
        respons.kjønn shouldBe Kjønn.MANN
        respons.fødselsdato shouldBe LocalDate.of(1955, 9, 13)
        respons.dødsdato shouldBe null
    }

    @Test
    fun `hentPersoninfo skal hente info for en død person`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdlOkResponseDødPerson.json"))),
        )

        val respons = pdlClient.hentPersoninfo("11111122222", Fagsystem.BA)

        respons.shouldNotBeNull()
        respons.navn shouldBe "ENGASJERT FYR"
        respons.kjønn shouldBe Kjønn.MANN
        respons.fødselsdato shouldBe LocalDate.of(1955, 9, 13)
        respons.dødsdato shouldBe LocalDate.of(2022, 4, 1)
    }

    @Test
    fun `hentPersoninfo skal ikke hente person info når person ikke finnes`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdlPersonIkkeFunnetResponse.json"))),
        )

        val exception = shouldThrow<RuntimeException>(
            block =
            { pdlClient.hentPersoninfo("11111122222", Fagsystem.BA) },
        )
        exception.message shouldBe "Feil ved oppslag på person: Person ikke funnet"
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/pdl/json/$filnavn").readText()
    }
}
