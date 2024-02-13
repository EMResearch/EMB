package no.nav.familie.ba.sak.integrasjoner.pdl

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.mockSjekkTilgang
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

internal class PersonopplysningerServiceTest(
    @Autowired
    @Qualifier("jwtBearer")
    private val restTemplate: RestOperations,

    @Autowired
    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,

    @Autowired
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,

    @Autowired
    private val mockPersonidentService: PersonidentService,

) : AbstractSpringIntegrationTest() {

    lateinit var personopplysningerService: PersonopplysningerService

    @BeforeEach
    fun setUp() {
        personopplysningerService =
            PersonopplysningerService(
                PdlRestClient(URI.create(wireMockServer.baseUrl() + "/api"), restTemplate, mockPersonidentService),
                SystemOnlyPdlRestClient(
                    URI.create(wireMockServer.baseUrl() + "/api"),
                    restTemplate,
                    mockPersonidentService,
                ),
                familieIntegrasjonerTilgangskontrollService,
            )
        lagMockForPersoner()
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon() skal return riktig personinfo`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(mapOf(ID_BARN_1 to true, ID_BARN_2 to false))

        val personInfo = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(ID_MOR))

        assert(LocalDate.of(1955, 9, 13) == personInfo.fødselsdato)
        assertThat(personInfo.adressebeskyttelseGradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        assertThat(personInfo.forelderBarnRelasjon.size).isEqualTo(1)
        assertThat(personInfo.forelderBarnRelasjonMaskert.size).isEqualTo(1)
        assertThat(personInfo.kontaktinformasjonForDoedsbo).isNull()
        assertThat(personInfo.dødsfall).isNull()
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon() skal returnere riktig personinfo for død person`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(mapOf(ID_BARN_1 to true, ID_BARN_2 to false))

        val personInfo = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
            tilAktør(
                ID_DØD_MOR,
            ),
        )

        assertThat(personInfo.dødsfall?.erDød).isTrue
        assertThat(personInfo.dødsfall?.dødsdato).isEqualTo("2020-04-04")
        assertThat(personInfo.kontaktinformasjonForDoedsbo?.adresse?.postnummer).isEqualTo("1234")
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon() skal filtrere bort relasjoner med opphørte folkreregisteridenter eller uten fødselsdato`() {
        mockFamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)

        val personInfo = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
            tilAktør(
                ID_MOR_3BARN_1OPPHØRT_1UTENFØDSELSDATO,
            ),
        )

        assertEquals(1, personInfo.forelderBarnRelasjon.size)
        assertEquals(ID_BARN_1, personInfo.forelderBarnRelasjon.single().aktør.aktivFødselsnummer())
    }

    @Test
    fun `hentStatsborgerskap() skal return riktig statsborgerskap`() {
        val statsborgerskap = personopplysningerService.hentGjeldendeStatsborgerskap(tilAktør(ID_MOR))
        assert(statsborgerskap.land == "XXX")
    }

    @Test
    fun `hentOpphold() skal returnere riktig opphold`() {
        val opphold = personopplysningerService.hentGjeldendeOpphold(tilAktør(ID_MOR))
        assert(opphold.type == OPPHOLDSTILLATELSE.MIDLERTIDIG)
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere landkode `() {
        val landkode = personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(tilAktør(ID_MOR))
        assertThat(landkode).isEqualTo("GB")
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere ZZ hvis ingen landkode `() {
        val landkode = personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(tilAktør(ID_BARN_1))
        assertThat(landkode).isEqualTo("ZZ")
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere ZZ hvis ingen bostedsadresse `() {
        val landkode =
            personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(tilAktør(ID_MOR_MED_TOM_BOSTEDSADRESSE))
        assertThat(landkode).isEqualTo("ZZ")
    }

    @Test
    fun `hentadressebeskyttelse skal returnere gradering`() {
        val gradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(tilAktør(ID_BARN_1))
        assertThat(gradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
    }

    @Test
    fun `hentadressebeskyttelse skal returnere ugradert ved tom liste fra pdl`() {
        val gradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(tilAktør(ID_UGRADERT_PERSON))
        assertThat(gradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
    }

    @Test
    fun `hentadressebeskyttelse feiler`() {
        assertThrows<HttpClientErrorException.NotFound> {
            personopplysningerService.hentAdressebeskyttelseSomSystembruker(
                tilAktør(
                    ID_MOR,
                ),
            )
        }
    }

    companion object {

        const val ID_MOR = "22345678901"
        const val ID_MOR_MED_TOM_BOSTEDSADRESSE = "22345678903"
        const val ID_DØD_MOR = "44556612345"
        const val ID_MOR_3BARN_1OPPHØRT_1UTENFØDSELSDATO = "94556612349"
        const val ID_BARN_1 = "32345678901"
        const val ID_BARN_2 = "32345678902"
        const val ID_UGRADERT_PERSON = "32345678903"
    }

    private fun gyldigRequest(queryFilnavn: String, requestFilnavn: String): String {
        return readfile(requestFilnavn)
            .replace(
                "GRAPHQL-PLACEHOLDER",
                readfile(queryFilnavn).graphqlCompatible(),
            )
    }

    private fun readfile(filnavn: String): String {
        return this::class.java.getResource("/pdl/$filnavn")!!.readText()
    }

    private fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }

    private fun lagMockForPdl(graphqlQueryFilnavn: String, requestFilnavn: String, mockResponse: String) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/graphql"))
                .withRequestBody(WireMock.equalToJson(gyldigRequest(graphqlQueryFilnavn, requestFilnavn)))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse),
                ),
        )
    }

    private fun lagMockForPersoner() {
        lagMockForPdl(
            "hentperson-med-relasjoner-og-registerinformasjon.graphql",
            "PdlIntegrasjon/gyldigRequestForMor3Barn1Opphørt1UtenFødselsdato.json",
            readfile("PdlIntegrasjon/personinfoResponseForMor3Barn1Opphørt1UtenFødselsdato.json"),
        )

        lagMockForPdl(
            "hentperson-med-relasjoner-og-registerinformasjon.graphql",
            "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"),
        )

        lagMockForPdl(
            "hentperson-enkel.graphql",
            "PdlIntegrasjon/gyldigRequestForBarn.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarn.json"),
        )

        lagMockForPdl(
            "hentperson-enkel.graphql",
            "PdlIntegrasjon/gyldigRequestForBarnUtenFødselsdato.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarnUtenFødselsdato.json"),
        )

        lagMockForPdl(
            "hentperson-enkel.graphql",
            "PdlIntegrasjon/gyldigRequestForBarnMedOpphørtStatus.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarnMedOpphørtStatus.json"),
        )

        lagMockForPdl(
            "hentperson-enkel.graphql",
            "PdlIntegrasjon/gyldigRequestForBarn2.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarnMedAdressebeskyttelse.json"),
        )

        lagMockForPdl(
            "hentperson-med-relasjoner-og-registerinformasjon.graphql",
            "PdlIntegrasjon/gyldigRequestForDødMor.json",
            readfile("PdlIntegrasjon/personinfoResponseForDødMor.json"),
        )

        lagMockForPdl(
            "statsborgerskap-uten-historikk.graphql",
            "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"),
        )

        lagMockForPdl(
            "opphold-uten-historikk.graphql",
            "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"),
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql",
            "PdlIntegrasjon/gyldigRequestForBostedsadresseperioder.json",
            readfile("PdlIntegrasjon/utenlandskAdresseResponse.json"),
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql",
            "PdlIntegrasjon/gyldigRequestForBarn.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarn.json"),
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql",
            "PdlIntegrasjon/gyldigRequestForMorMedTomBostedsadresse.json",
            readfile("PdlIntegrasjon/tomBostedsadresseResponse.json"),
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql",
            "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse.json",
            readfile("pdlAdressebeskyttelseResponse.json"),
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql",
            "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse2.json",
            readfile("pdlAdressebeskyttelseResponse.json"),
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql",
            "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse3.json",
            readfile("PdlIntegrasjon/pdlAdressebeskyttelseMedTomListeResponse.json"),
        )
    }
}
