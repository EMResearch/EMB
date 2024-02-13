package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.mockSjekkTilgang
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InfotrygdControllerTest {
    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var systemOnlyPdlRestClient: SystemOnlyPdlRestClient

    @SpyK
    var cacheManager = ConcurrentMapCacheManager()

    @MockK
    lateinit var familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

    @InjectMockKs
    lateinit var familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService

    @MockK
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @MockK
    lateinit var personidentService: PersonidentService

    @InjectMockKs
    lateinit var infotrygdService: InfotrygdService

    lateinit var infotrygdController: InfotrygdController

    @BeforeAll
    fun init() {
        infotrygdController = InfotrygdController(infotrygdBarnetrygdClient, personidentService, infotrygdService)
    }

    @BeforeEach
    fun setUp() {
        cacheManager.clearAllCaches()
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok dersom saksbehandler har tilgang`() {
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns tilAktør(fnr)
        familieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(true)
        every {
            infotrygdBarnetrygdClient.hentSaker(
                any(),
                any(),
            )
        } returns InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())
        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(true, respons.body?.data?.harTilgang)
        Assertions.assertEquals("IP", respons.body?.data?.saker!![0].status)
    }

    @Test
    fun `hentInfotrygdsakerForSøker skal returnere ok, men ha gradering satt, dersom saksbehandler ikke har tilgang`() {
        val fnr = "12345678910"

        every { personidentService.hentAktør(fnr) } returns tilAktør(fnr)
        familieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(false)
        every { systemOnlyPdlRestClient.hentAdressebeskyttelse(any()) } returns
            listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG))

        val respons = infotrygdController.hentInfotrygdsakerForSøker(Personident(fnr))

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(false, respons.body?.data?.harTilgang)
        Assertions.assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, respons.body?.data?.adressebeskyttelsegradering)
    }
}
