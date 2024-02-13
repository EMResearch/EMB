package no.nav.familie.tilbake.integration.økonomi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.FeilutbetalingerFraSimulering
import no.nav.familie.kontrakter.felles.simulering.FeilutbetaltPeriode
import no.nav.familie.kontrakter.felles.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.iverksettvedtak.TilbakekrevingsvedtakMarshaller
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.HentKravgrunnlagDetaljDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.time.YearMonth
import java.util.UUID

internal class OppdragClientTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var oppdragClient: OppdragClient

    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private lateinit var tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest
    private lateinit var hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest
    private val kravgrunnlagId: BigInteger = BigInteger.ZERO

    @BeforeEach
    fun init() {
        wireMockServer.start()

        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        oppdragClient = DefaultOppdragClient(restOperations, URI.create(wireMockServer.baseUrl()))

        val tilbakekrevingsvedtakRequestXml = readXml("/tilbakekrevingsvedtak/tilbakekrevingsvedtak.xml")
        tilbakekrevingsvedtakRequest = TilbakekrevingsvedtakMarshaller.unmarshall(
            tilbakekrevingsvedtakRequestXml,
            behandling.id,
            UUID.randomUUID(),
        )
        hentKravgrunnlagRequest = KravgrunnlagHentDetaljRequest().apply {
            hentkravgrunnlag = HentKravgrunnlagDetaljDto().apply {
                kravgrunnlagId = kravgrunnlagId
                kodeAksjon = KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG.kode
                saksbehId = "testverdi"
                enhetAnsvarlig = "testverdi"
            }
        }
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `iverksettVedtak skal sende iverksettelse request til oppdrag`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${DefaultOppdragClient.IVERKSETTELSE_PATH}/${behandling.id}"))
                .willReturn(okJson(Ressurs.success(lagIverksettelseRespons()).toJson())),
        )
        val iverksettVedtak = oppdragClient.iverksettVedtak(behandling.id, tilbakekrevingsvedtakRequest)

        iverksettVedtak shouldNotBe null
    }

    @Test
    fun `iverksettVedtak skal ikke sende iverksettelse request til oppdrag når oppdrag har nedetid`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${DefaultOppdragClient.IVERKSETTELSE_PATH}/${behandling.id}"))
                .willReturn(status(HttpStatus.REQUEST_TIMEOUT_408)),
        )

        val exception = shouldThrow<RuntimeException> {
            oppdragClient.iverksettVedtak(
                behandling.id,
                tilbakekrevingsvedtakRequest,
            )
        }
        exception.shouldNotBeNull()
        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved iverksetting av behandling=${behandling.id}"
    }

    @Test
    fun `iverksettVedtak skal ikke iverksette behandling til oppdrag når økonomi ikke svarer`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${DefaultOppdragClient.IVERKSETTELSE_PATH}/${behandling.id}"))
                .willReturn(serviceUnavailable().withStatusMessage("Couldn't send message")),
        )

        val exception = shouldThrow<RuntimeException> {
            oppdragClient.iverksettVedtak(
                behandling.id,
                tilbakekrevingsvedtakRequest,
            )
        }
        exception.shouldNotBeNull()
        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved iverksetting av behandling=${behandling.id}"
        exception.cause?.message shouldBe "503 Couldn't send message: [no body]"
    }

    @Test
    fun `hentKravgrunnlag skal hente kravgrunnlag fra oppdrag`() {
        wireMockServer.stubFor(
            post(
                urlEqualTo(
                    "/${DefaultOppdragClient.HENT_KRAVGRUNNLAG_PATH}/$kravgrunnlagId",
                ),
            )
                .willReturn(
                    okJson(
                        Ressurs.success(
                            lagHentKravgrunnlagRespons(
                                "00",
                                "OK",
                            ),
                        )
                            .toJson(),
                    ),
                ),
        )
        val hentKravgrunnlag = oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)

        hentKravgrunnlag shouldNotBe null
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når kravgrunnlag ikke finnes i økonomi`() {
        wireMockServer.stubFor(
            post(
                urlEqualTo(
                    "/${DefaultOppdragClient.HENT_KRAVGRUNNLAG_PATH}/$kravgrunnlagId",
                ),
            )
                .willReturn(
                    okJson(
                        Ressurs.success(
                            lagHentKravgrunnlagRespons(
                                "00",
                                "B420010I",
                            ),
                        )
                            .toJson(),
                    ),
                ),
        )
        val exception = shouldThrow<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        exception.shouldNotBeNull()
        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId"
        exception.cause?.message shouldBe "Fikk feil respons:{\"systemId\":null,\"kodeMelding\":\"B420010I\"," +
            "\"alvorlighetsgrad\":\"00\",\"beskrMelding\":null,\"sqlKode\":null,\"sqlState\":null,\"sqlMelding\":null," +
            "\"mqCompletionKode\":null,\"mqReasonKode\":null,\"programId\":null,\"sectionNavn\":null} fra økonomi " +
            "ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId."
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når kravgrunnlag er sperret i økonomi`() {
        wireMockServer.stubFor(
            post(
                urlEqualTo(
                    "/${DefaultOppdragClient.HENT_KRAVGRUNNLAG_PATH}/$kravgrunnlagId",
                ),
            )
                .willReturn(
                    okJson(
                        Ressurs.success(
                            lagHentKravgrunnlagRespons(
                                "00",
                                "B420012I",
                            ),
                        )
                            .toJson(),
                    ),
                ),
        )
        val exception = shouldThrow<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        exception.shouldNotBeNull()
        exception.message shouldBe "Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId"
        exception.cause?.message shouldBe "Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret"
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når økonomi ikke svarer`() {
        wireMockServer.stubFor(
            post(
                urlEqualTo(
                    "/${DefaultOppdragClient.HENT_KRAVGRUNNLAG_PATH}/$kravgrunnlagId",
                ),
            )
                .willReturn(serviceUnavailable().withStatusMessage("Couldn't send message")),
        )
        val exception = shouldThrow<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        exception.shouldNotBeNull()
        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId"
        exception.cause?.message shouldBe "503 Couldn't send message: [no body]"
    }

    @Test
    fun `hentFeilutbetalingerFraSimulering skal hente feilutbetalinger fra simulering`() {
        val feilutbetaltPeriode = FeilutbetaltPeriode(
            fom = YearMonth.now().minusMonths(2).atDay(1),
            tom = YearMonth.now().minusMonths(1).atDay(1),
            feilutbetaltBeløp = BigDecimal("20000"),
            tidligereUtbetaltBeløp = BigDecimal("30000"),
            nyttBeløp = BigDecimal("10000"),
        )
        val feilutbetaltPerioder = FeilutbetalingerFraSimulering(listOf(feilutbetaltPeriode))
        wireMockServer.stubFor(
            post(urlEqualTo("/${DefaultOppdragClient.HENT_FEILUTBETALINGER_PATH}"))
                .willReturn(okJson(Ressurs.success(feilutbetaltPerioder).toJson())),
        )

        val respons = oppdragClient
            .hentFeilutbetalingerFraSimulering(
                HentFeilutbetalingerFraSimuleringRequest(
                    Ytelsestype.OVERGANGSSTØNAD,
                    "123",
                    "1",
                ),
            )
        respons shouldNotBe null
    }

    @Test
    fun `hentFeilutbetalingerFraSimulering skal ikke hente feilutbetalinger fra simulering`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${DefaultOppdragClient.HENT_FEILUTBETALINGER_PATH}"))
                .willReturn(serviceUnavailable().withStatusMessage("Couldn't send message")),
        )

        val exception = shouldThrow<RuntimeException> {
            oppdragClient.hentFeilutbetalingerFraSimulering(
                HentFeilutbetalingerFraSimuleringRequest(
                    Ytelsestype.OVERGANGSSTØNAD,
                    "123",
                    "1",
                ),
            )
        }
        exception.shouldNotBeNull()
        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved henting av feilutbetalinger fra simulering"
        exception.cause?.message shouldBe "503 Couldn't send message: [no body]"
    }

    private fun lagIverksettelseRespons(): TilbakekrevingsvedtakResponse {
        val mmelDto = lagMmmelDto("00", "OK")

        val respons = TilbakekrevingsvedtakResponse()
        respons.mmel = mmelDto
        respons.tilbakekrevingsvedtak = tilbakekrevingsvedtakRequest.tilbakekrevingsvedtak

        return respons
    }

    private fun lagHentKravgrunnlagRespons(
        alvorlighetsgrad: String,
        kodeMelding: String,
    ): KravgrunnlagHentDetaljResponse {
        val mmelDto = lagMmmelDto(alvorlighetsgrad, kodeMelding)

        val respons = KravgrunnlagHentDetaljResponse()
        respons.mmel = mmelDto
        respons.detaljertkravgrunnlag = DetaljertKravgrunnlagDto()
        respons.detaljertkravgrunnlag = KravgrunnlagUtil
            .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        return respons
    }

    private fun lagMmmelDto(alvorlighetsgrad: String, kodeMelding: String): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = alvorlighetsgrad
        mmelDto.kodeMelding = kodeMelding
        return mmelDto
    }
}
