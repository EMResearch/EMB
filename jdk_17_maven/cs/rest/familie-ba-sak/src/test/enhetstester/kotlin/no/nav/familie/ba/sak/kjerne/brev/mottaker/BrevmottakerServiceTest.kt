package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull

@ExtendWith(MockKExtension::class)
internal class BrevmottakerServiceTest {

    @MockK
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    private lateinit var validerBrevmottakerService: ValiderBrevmottakerService

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var brevmottakerService: BrevmottakerService

    private val søkersident = "123"
    private val søkersnavn = "Test søker"

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
        val brevmottakere = listOf(lagBrevMottaker(mottakerType = MottakerType.FULLMEKTIG))
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo == null }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(mottakerType = MottakerType.FULLMEKTIG),
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE",
            ),
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(mottakerType = MottakerType.VERGE),
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE",
            ),
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE",
            ),
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 1 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har dødsbo`() {
        val brevmottakere = listOf(
            lagBrevMottaker(
                mottakerType = MottakerType.DØDSBO,
                poststed = "Munchen",
                landkode = "DE",
            ),
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 1 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal kaste feil når brevmottakere inneholder ugyldig kombinasjon`() {
        val brevmottakere = listOf(
            lagBrevMottaker(
                mottakerType = MottakerType.VERGE,
                poststed = "Munchen",
                landkode = "DE",
            ),
            lagBrevMottaker(
                mottakerType = MottakerType.FULLMEKTIG,
                poststed = "Munchen",
                landkode = "DE",
            ),
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        assertThrows<FunksjonellFeil> {
            brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        }.also {
            assertTrue(it.frontendFeilmelding!!.contains("kan ikke kombineres"))
        }
    }

    @Test
    fun `leggTilBrevmottaker skal lagre logg på at brevmottaker legges til`() {
        val restBrevmottaker = mockk<RestBrevmottaker>(relaxed = true)

        every { validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(any(), any()) } just runs
        every { loggService.opprettBrevmottakerLogg(any(), false) } just runs
        every { brevmottakerRepository.save(any()) } returns mockk()

        brevmottakerService.leggTilBrevmottaker(restBrevmottaker, 200)

        verify { loggService.opprettBrevmottakerLogg(any(), false) }
        verify { brevmottakerRepository.save(any()) }
    }

    @Test
    fun `fjernBrevmottaker skal kaste feil dersom brevmottakeren ikke finnes`() {
        every { brevmottakerRepository.findByIdOrNull(404) } returns null

        assertThrows<Feil> {
            brevmottakerService.fjernBrevmottaker(404)
        }

        verify { brevmottakerRepository.findByIdOrNull(404) }
    }

    @Test
    fun `fjernBrevmottaker skal lagre logg på at brevmottaker fjernes`() {
        val mocketBrevmottaker = mockk<Brevmottaker>()

        every { brevmottakerRepository.findByIdOrNull(200) } returns mocketBrevmottaker
        every { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) } just runs
        every { brevmottakerRepository.deleteById(200) } just runs

        brevmottakerService.fjernBrevmottaker(200)

        verify { brevmottakerRepository.findByIdOrNull(200) }
        verify { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) }
        verify { brevmottakerRepository.deleteById(200) }
    }

    private fun lagBrevMottaker(mottakerType: MottakerType, poststed: String = "Oslo", landkode: String = "NO") =
        Brevmottaker(
            behandlingId = 1,
            type = mottakerType,
            navn = "John Doe",
            adresselinje1 = "adresse 1",
            adresselinje2 = "adresse 2",
            postnummer = "000",
            poststed = poststed,
            landkode = landkode,
        )
}
