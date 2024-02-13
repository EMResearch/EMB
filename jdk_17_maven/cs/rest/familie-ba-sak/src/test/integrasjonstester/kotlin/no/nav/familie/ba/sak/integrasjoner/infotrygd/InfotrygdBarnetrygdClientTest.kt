package no.nav.familie.ba.sak.integrasjoner.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

class InfotrygdBarnetrygdClientTest : AbstractSpringIntegrationTest() {

    val løpendeBarnetrygdURL = "/api/infotrygd/barnetrygd/lopende-barnetrygd"
    val sakerURL = "/api/infotrygd/barnetrygd/saker"
    val stønaderURL = "/api/infotrygd/barnetrygd/stonad?historikk=false"
    val brevURL = "/api/infotrygd/barnetrygd/brev"

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @Autowired
    lateinit var environment: Environment

    lateinit var client: InfotrygdBarnetrygdClient

    @BeforeEach
    fun setUp() {
        client = InfotrygdBarnetrygdClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations,
        )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr og barns fødselsnummer`() {
        wireMockServer.stubFor(
            post(løpendeBarnetrygdURL).willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        InfotrygdLøpendeBarnetrygdResponse(false),
                    ),
                ),
            ),
        )
        wireMockServer.stubFor(
            post(sakerURL).willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList()),
                    ),
                ),
            ),
        )
        wireMockServer.stubFor(
            post(stønaderURL).willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        InfotrygdSøkResponse(listOf(Stønad()), emptyList()),
                    ),
                ),
            ),
        )

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val barnasIdenter = ClientMocks.barnFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        val finnesIkkeHosInfotrygd = client.harLøpendeSakIInfotrygd(søkersIdenter, barnasIdenter)
        val hentsakerResponse = client.hentSaker(søkersIdenter, barnasIdenter)
        val hentstønaderResponse = client.hentStønader(søkersIdenter, barnasIdenter)

        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(løpendeBarnetrygdURL)).withRequestBody(
                equalToJson(
                    objectMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(sakerURL)).withRequestBody(
                equalToJson(
                    objectMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(stønaderURL)).withRequestBody(
                equalToJson(
                    objectMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        Assertions.assertEquals(false, finnesIkkeHosInfotrygd)
        Assertions.assertEquals(hentsakerResponse.bruker[0].status, "IP")
        Assertions.assertEquals(hentstønaderResponse.bruker.size, 1)
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr`() {
        wireMockServer.stubFor(
            post(løpendeBarnetrygdURL).willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        InfotrygdLøpendeBarnetrygdResponse(false),
                    ),
                ),
            ),
        )

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, emptyList())

        val finnesIkkeHosInfotrygd = client.harLøpendeSakIInfotrygd(søkersIdenter, emptyList())

        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(løpendeBarnetrygdURL)).withRequestBody(
                equalToJson(
                    objectMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        Assertions.assertEquals(false, finnesIkkeHosInfotrygd)
    }

    @Test
    fun `Invokering av Infotrygd-Barnetrygd genererer http feil`() {
        wireMockServer.stubFor(post(løpendeBarnetrygdURL).willReturn(aResponse().withStatus(401)))

        assertThrows<HttpClientErrorException> {
            client.harLøpendeSakIInfotrygd(ClientMocks.søkerFnr.toList(), ClientMocks.barnFnr.toList())
        }
    }

    @Test
    fun `harNyligSendtBrevFor skal returnerer true for personIdent`() {
        wireMockServer.stubFor(
            post(brevURL).willReturn(
                okJson(objectMapper.writeValueAsString(InfotrygdBarnetrygdClient.SendtBrevResponse(true, emptyList()))),
            ),
        )

        val søkersIdenter = ClientMocks.søkerFnr.toList()

        val harNyligSendtBrev = client.harNyligSendtBrevFor(
            søkersIdenter,
            listOf(InfotrygdBrevkode.BREV_BATCH_INNVILGET_SMÅBARNSTILLEGG),
        )

        Assertions.assertEquals(true, harNyligSendtBrev.harSendtBrev)
    }
}
