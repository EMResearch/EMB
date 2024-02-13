package no.nav.familie.ba.sak.kjerne.institusjon

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.samhandlereInfoMock
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

internal class SamhandlerControllerTest {

    lateinit var samhandlerController: SamhandlerController
    private val samhandlerKlientMock: SamhandlerKlient = mockk()
    private val institusjonRepository: InstitusjonRepository = mockk()

    @BeforeEach
    fun setUp() {
        val institusjonService = InstitusjonService(mockk(), samhandlerKlientMock, institusjonRepository)
        samhandlerController = SamhandlerController(institusjonService = institusjonService)
        clearMocks(samhandlerKlientMock)
    }

    @Test
    fun `Skal hente samhandlerinformasjon fra orgnr `() {
        every { samhandlerKlientMock.hentSamhandler(any()) } returns samhandlereInfoMock.first()

        val samhandlerInfo = samhandlerController.hentSamhandlerDataForOrganisasjon("ORGNR")
        assertThat(samhandlerInfo.data).isNotNull()
        assertThat(samhandlerInfo.data!!.tssEksternId).isEqualTo("80000999999")
    }

    @Test
    fun `Kaster feilmelding hvis det ikke fins organisasjon med gitt orgnr`() {
        every { samhandlerKlientMock.hentSamhandler(any()) } throws HttpClientErrorException(HttpStatus.NOT_FOUND)
        val feil = assertThrows<FunksjonellFeil> {
            samhandlerController.hentSamhandlerDataForOrganisasjon("123456789")
        }
        assertThat(feil.message).isEqualTo("Finner ikke institusjon. Kontakt NØS for å opprette TSS-ident.")
    }

    @Test
    fun `Søk etter samhandlere skal returnere samhandlere på navn og ikke hente flere hvis det ikke finnes flere samhandlere`() {
        every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 0) } returns SøkSamhandlerInfo(
            false,
            samhandlereInfoMock,
        )

        val samhandlerInfo =
            samhandlerController.søkSamhandlerinfoFraNavn(SøkSamhandlerInfoRequest("Bufetat", null, null))
        assertThat(samhandlerInfo.data).isNotNull()
        assertThat(samhandlerInfo.data).hasSize(2)
        assertThat(samhandlerInfo.data?.get(0)?.tssEksternId).isEqualTo("80000999999")
        assertThat(samhandlerInfo.data?.get(1)?.tssEksternId).isEqualTo("80000888888")
        verify(exactly = 1) { samhandlerKlientMock.søkSamhandlere(any(), any(), any(), any()) }
    }

    @Test
    fun `Søk etter samhandlere skal returnere samhandlere på navn og slå sammen resultatene fra alle sidene ved mer enn 1 side`() {
        every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 0) } returns SøkSamhandlerInfo(
            true,
            listOf(samhandlereInfoMock.get(0)),
        )

        every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 1) } returns SøkSamhandlerInfo(
            false,
            listOf(samhandlereInfoMock.get(1)),
        )

        val samhandlerInfo =
            samhandlerController.søkSamhandlerinfoFraNavn(SøkSamhandlerInfoRequest("Bufetat", null, null))
        assertThat(samhandlerInfo.data).isNotNull()
        assertThat(samhandlerInfo.data).hasSize(2)
        assertThat(samhandlerInfo.data?.get(0)?.tssEksternId).isEqualTo("80000999999")
        assertThat(samhandlerInfo.data?.get(1)?.tssEksternId).isEqualTo("80000888888")
        verify(exactly = 2) { samhandlerKlientMock.søkSamhandlere(any(), any(), any(), any()) }
    }
}
