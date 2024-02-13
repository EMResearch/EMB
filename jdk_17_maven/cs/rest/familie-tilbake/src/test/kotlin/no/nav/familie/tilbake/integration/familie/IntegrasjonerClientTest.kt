package no.nav.familie.tilbake.integration.familie

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

internal class IntegrasjonerClientTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private val restOperations: RestOperations = RestTemplateBuilder().build()

    private lateinit var integrasjonerClient: IntegrasjonerClient
    private val arkiverDokumentRequest = ArkiverDokumentRequest("123456789", true, listOf())

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        integrasjonerClient = IntegrasjonerClient(
            restOperations,
            IntegrasjonerConfig(URI.create(wireMockServer.baseUrl()), "tilbake"),
        )
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `arkiver skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        val arkiverDokumentResponse = ArkiverDokumentResponse("wer", true)

        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerConfig.PATH_ARKIVER}"))
                .willReturn(okJson(success(arkiverDokumentResponse).toJson())),
        )

        integrasjonerClient.arkiver(arkiverDokumentRequest).shouldNotBeNull()
    }

    @Test
    fun `arkiver skal kaste feil hvis hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerConfig.PATH_ARKIVER}"))
                .willReturn(okJson(failure<Any>("error").toJson())),
        )

        shouldThrow<IllegalStateException> {
            integrasjonerClient.arkiver(arkiverDokumentRequest)
        }
    }

    @Test
    fun `distribuerJournalpost skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        // Gitt
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerConfig.PATH_DISTRIBUER}"))
                .willReturn(okJson(success("id").toJson())),
        )
        // Vil gi resultat
        integrasjonerClient.distribuerJournalpost(
            "3216354",
            Fagsystem.EF,
            Distribusjonstype.VIKTIG,
            Distribusjonstidspunkt.KJERNETID,
        ).shouldNotBeNull()
    }

    @Test
    fun `distribuerJournalpost skal kaste feil hvis hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerConfig.PATH_DISTRIBUER}"))
                .willReturn(okJson(failure<Any>("error").toJson())),
        )

        shouldThrow<IllegalStateException> {
            integrasjonerClient.distribuerJournalpost(
                "3216354",
                Fagsystem.EF,
                Distribusjonstype.VIKTIG,
                Distribusjonstidspunkt.KJERNETID,
            )
        }
    }

    @Test
    fun `hentOrganisasjon skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        // Gitt
        wireMockServer.stubFor(
            get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321"))
                .willReturn(
                    okJson(
                        success(Organisasjon("Bob AS", "987654321"))
                            .toJson(),
                    ),
                ),
        )
        // Vil gi resultat
        integrasjonerClient.hentOrganisasjon("987654321").shouldNotBeNull()
    }

    @Test
    fun `hentOrganisasjon skal kaste feil hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321"))
                .willReturn(okJson(failure<Any>("error").toJson())),
        )

        shouldThrow<IllegalStateException> {
            integrasjonerClient.hentOrganisasjon("987654321")
        }
    }

    @Test
    fun `validerOrganisasjon skal gi vellykket respons hvis organisasjonnr er gyldig`() {
        // Gitt
        wireMockServer.stubFor(
            get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321/valider"))
                .willReturn(okJson(success(true).toJson())),
        )
        // Vil gi resultat
        integrasjonerClient.validerOrganisasjon("987654321").shouldBeTrue()
    }

    @Test
    fun `validerOrganisasjon skal kaste feil hvis organisasjonnr er ugyldig`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321/valider"))
                .willReturn(okJson(success(false).toJson())),
        )

        integrasjonerClient.validerOrganisasjon("987654321").shouldBeFalse()
    }
}
