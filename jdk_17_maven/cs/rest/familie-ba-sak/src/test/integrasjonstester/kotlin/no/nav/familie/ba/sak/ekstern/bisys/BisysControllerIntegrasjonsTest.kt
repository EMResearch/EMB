package no.nav.familie.ba.sak.ekstern.bisys

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.postForEntity
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@ActiveProfiles("postgres", "integrasjonstest", "mock-pdl", "mock-ident-client", "mock-oauth", "mock-brev-klient")
class BisysControllerIntegrasjonsTest : WebSpringAuthTestRunner() {

    // Trenger fast port for at klienten i ba-sak kan kalle wiremock'en
    private val wireMockServer = WireMockServer(28085)

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
    }

    @AfterEach
    fun after() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `Skal kaste feil når fraDato er mer enn 5 år tilbake i tid`() {
        val fnr = randomFnr()

        val requestEntity = byggRequestEntity(
            BisysUtvidetBarnetrygdRequest(
                fnr,
                LocalDate.now().minusYears(5).minusDays(1),
            ),
        )

        val error = assertThrows<HttpClientErrorException> {
            restTemplate.postForEntity<Any>(
                hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
                requestEntity,
            )
        }

        assertThat(error.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val errorObject = objectMapper.readValue<EksternTjenesteFeil>(error.responseBodyAsByteArray)
        assertThat(errorObject.melding).isEqualTo("fraDato kan ikke være lenger enn 5 år tilbake i tid")
        assertThat(errorObject.path).isEqualTo("/api/bisys/hent-utvidet-barnetrygd")
        assertThat(errorObject.timestamp).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
    }

    @Test
    fun `Skal kaste gode feilmeldinger ved feil mot infotrygd-barnetrygd`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("foobar"),
                ),
        )

        val requestEntity = byggRequestEntity(
            BisysUtvidetBarnetrygdRequest(
                fnr,
                LocalDate.now(),
            ),
        )

        val error = assertThrows<HttpServerErrorException> {
            restTemplate.postForEntity<Any>(
                hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
                requestEntity,
            )
        }

        assertThat(error.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val errorObject = objectMapper.readValue<EksternTjenesteFeil>(error.responseBodyAsByteArray)
        assertThat(errorObject.melding).isEqualTo("Henting av utvidet barnetrygd feilet. Gav feil: 500 Server Error: \"foobar\"")
        assertThat(errorObject.path).isEqualTo("/api/bisys/hent-utvidet-barnetrygd")
        assertThat(errorObject.timestamp).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(errorObject.exception).contains("HttpServerErrorException")
        assertThat(errorObject.stackTrace).isNotEmpty
    }

    @Test
    fun `Skal returnere tom periode hvis det ikke er noen utbetalinger i infotrygd-barnetrygd`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigOppgaveResponse("tom")),
                ),
        )

        val requestEntity = byggRequestEntity(
            BisysUtvidetBarnetrygdRequest(
                fnr,
                LocalDate.now().minusYears(4),
            ),
        )

        val responseEntity = restTemplate.postForEntity<BisysUtvidetBarnetrygdResponse>(
            hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
            requestEntity,
        )
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .withRequestBody(
                    equalToJson(
                        """{"personIdent":"$fnr", "fraDato":"${
                            YearMonth.now().minusYears(4)
                        }" }""",
                    ),
                ),

        )

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body!!.perioder).isEmpty()
    }

    @Test
    fun `Skal returnere perioder hvis det er noen utbetalinger i infotrygd-barnetrygd`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigOppgaveResponse("tom")),
                ),
        )

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .withRequestBody(
                    equalToJson(
                        """{"personIdent":"$fnr", "fraDato":"${
                            YearMonth.now().minusYears(4)
                        }" }""",
                    ),
                )
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigOppgaveResponse("med-perioder")),
                ),
        )

        val requestEntity = byggRequestEntity(
            BisysUtvidetBarnetrygdRequest(
                fnr,
                LocalDate.now().minusYears(4),
            ),
        )

        val responseEntity = restTemplate.postForEntity<BisysUtvidetBarnetrygdResponse>(
            hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
            requestEntity,
        )

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body!!.perioder)
            .hasSize(2)
        assertThat(responseEntity.body!!.perioder)
            .contains(
                UtvidetBarnetrygdPeriode(
                    BisysStønadstype.SMÅBARNSTILLEGG,
                    YearMonth.of(2019, 12),
                    null,
                    660.0,
                    false,
                ),
            )
        assertThat(responseEntity.body!!.perioder)
            .contains(UtvidetBarnetrygdPeriode(BisysStønadstype.UTVIDET, YearMonth.of(2019, 12), null, 1054.0, false))
    }

    @Test
    fun `Skal også returnere gamle perioder hvis det er noen utbetalinger i infotrygd-barnetrygd`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigOppgaveResponse("gammel-periode")),
                ),
        )

        wireMockServer.stubFor(
            post(urlEqualTo("/infotrygd/barnetrygd/utvidet"))
                .withRequestBody(
                    equalToJson(
                        """{"personIdent":"$fnr", "fraDato":"${
                            YearMonth.now().minusYears(4)
                        }" }""",
                    ),
                )
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigOppgaveResponse("med-perioder")),
                ),
        )

        val requestEntity = byggRequestEntity(
            BisysUtvidetBarnetrygdRequest(
                fnr,
                LocalDate.now().minusYears(4),
            ),
        )

        val responseEntity = restTemplate.postForEntity<BisysUtvidetBarnetrygdResponse>(
            hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
            requestEntity,
        )

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body!!.perioder)
            .hasSize(3)
        assertThat(responseEntity.body!!.perioder)
            .contains(
                UtvidetBarnetrygdPeriode(
                    BisysStønadstype.SMÅBARNSTILLEGG,
                    YearMonth.of(2019, 12),
                    null,
                    660.0,
                    false,
                ),
            )
        assertThat(responseEntity.body!!.perioder)
            .contains(UtvidetBarnetrygdPeriode(BisysStønadstype.UTVIDET, YearMonth.of(2019, 12), null, 1054.0, false))
        assertThat(responseEntity.body!!.perioder)
            .contains(
                UtvidetBarnetrygdPeriode(
                    BisysStønadstype.UTVIDET,
                    YearMonth.of(2017, 1),
                    YearMonth.of(2018, 12),
                    970.0,
                    false,
                ),
            )
    }

    @Test
    fun `Skal kaste feil tilgang når bisys kaller tjenste som ikke er bisys-relatert`() {
        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            hentTokenForBisys(),
        )
        val ikkeBisysTjeneste = HttpEntity<String>(
            "tullball",
            header,
        )

        val error = assertThrows<HttpClientErrorException> {
            restTemplate.postForEntity<Any>(
                hentUrl("/api/tullballtjeneste"),
                ikkeBisysTjeneste,
            )
        }

        assertThat(error.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    private fun byggRequestEntity(request: BisysUtvidetBarnetrygdRequest): HttpEntity<String> {
        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            hentTokenForBisys(),
        )
        return HttpEntity(
            objectMapper.writeValueAsString(
                request,
            ),
            header,
        )
    }

    private fun hentTokenForBisys() = token(
        mapOf(
            "groups" to listOf("SAKSBEHANDLER"),
            "name" to "Mock McMockface",
            "NAVident" to "Z0000",
        ),
        clientId = "dummy",
    )

    private fun gyldigOppgaveResponse(filnavn: String): String {
        return Files.readString(
            ClassPathResource("ekstern/bisys-$filnavn.json").file.toPath(),
            StandardCharsets.UTF_8,
        )
    }
}
