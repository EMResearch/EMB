package no.nav.familie.tilbake.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.person.PersonService
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*

class ValiderBrevmottakerServiceTest {
    private val manuellBrevmottakerRepository = mockk<ManuellBrevmottakerRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val personService = mockk<PersonService>()
    val validerBrevmottakerService = ValiderBrevmottakerService(
        manuellBrevmottakerRepository,
        fagsakService,
        personService,
    )
    private val behandlingId = UUID.randomUUID()
    private val fagsak = Testdata.fagsak
    private val manuellBrevmottaker = ManuellBrevmottaker(
        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
        behandlingId = behandlingId,
        navn = "Donald Duck",
        adresselinje1 = "adresselinje1",
        postnummer = "postnummer",
        poststed = "poststed",
        landkode = "NO",
    )

    @Test
    fun `Skal ikke kaste en Feil exception når en behandling ikke inneholder noen manuelle brevmottakere`() {
        every { manuellBrevmottakerRepository.findByBehandlingId(any()) } returns emptyList()
        assertThatNoException().isThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId,
                fagsak.id,
            )
        }
    }

    @Test
    fun `Skal kaste en Feil exception når en behandling inneholder en strengt fortrolig person og minst en manuell brevmottaker`() {
        every { manuellBrevmottakerRepository.findByBehandlingId(behandlingId) } returns listOf(manuellBrevmottaker)
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns listOf(
            fagsak.bruker.ident,
        )
        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId,
                fagsak.id,
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }

    @Test
    fun `Skal ikke kaste Feil exception når behandling ikke inneholder strengt fortrolig person og inneholder en manuell brevmottaker`() {
        every { manuellBrevmottakerRepository.findByBehandlingId(behandlingId) } returns listOf(manuellBrevmottaker)
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns emptyList()
        assertThatNoException().isThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId,
                fagsak.id,
            )
        }
    }

    @Test
    fun `Skal ikke kaste Feil exception når en behandling inneholder strengt fortrolig person og ingen manuelle brevmottakere`() {
        every { manuellBrevmottakerRepository.findByBehandlingId(behandlingId) } returns emptyList()
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns listOf(
            fagsak.bruker.ident,
        )
        assertThatNoException().isThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId,
                fagsak.id,
            )
        }
    }

    @Test
    fun `Skal ikke kaste en Feil exception når en behandling ikke inneholder en strengt fortrolig person`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns emptyList()
        assertThatNoException().isThrownBy {
            validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(
                behandlingId,
                fagsak.id,
            )
        }
    }

    @Test
    fun `Skal kaste en Feil exception når en behandling inneholder en strengt fortrolig person`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns listOf(
            fagsak.bruker.ident,
        )
        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(
                behandlingId,
                fagsak.id,
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }
}
