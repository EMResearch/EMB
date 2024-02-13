package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.journalføring.InnkommendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.RestFinnOppgaveRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveControllerTest {

    @MockK
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var personidentService: PersonidentService

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var fagsakService: FagsakService

    @MockK
    lateinit var innkommendeJournalføringService: InnkommendeJournalføringService

    @MockK
    lateinit var tilgangService: TilgangService

    @InjectMockKs
    lateinit var oppgaveController: OppgaveController

    @BeforeAll
    fun init() {
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    }

    @Test
    fun `Tildeling av oppgave til saksbehandler skal returnere OK og sende med OppgaveId i respons`() {
        val OPPGAVE_ID = "1234"
        val SAKSBEHANDLER_ID = "Z999999"
        every { oppgaveService.fordelOppgave(any(), any()) } returns OPPGAVE_ID

        val respons = oppgaveController.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(OPPGAVE_ID, respons.body?.data)
    }

    @Test
    fun `Tilbakestilling av tildeling på oppgave skal returnere OK og sende med Oppgave i respons`() {
        val oppgave = Oppgave(
            id = 1234,
        )
        every { oppgaveService.tilbakestillFordelingPåOppgave(oppgave.id!!) } returns oppgave

        val respons = oppgaveController.tilbakestillFordelingPåOppgave(oppgave.id!!)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgave, respons.body?.data)
    }

    @Test
    fun `Tildeling av oppgave skal returnere feil ved feil fra integrasjonsklienten`() {
        val OPPGAVE_ID = "1234"
        val SAKSBEHANDLER_ID = "Z999998"
        every {
            oppgaveService.fordelOppgave(
                any(),
                any(),
            )
        } throws IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave")

        val exception = assertThrows<IntegrasjonException> {
            oppgaveController.fordelOppgave(
                OPPGAVE_ID.toLong(),
                SAKSBEHANDLER_ID,
            )
        }

        Assertions.assertEquals("Kall mot integrasjon feilet ved fordel oppgave", exception.message)
    }

    @Test
    fun `hentOppgaver via OppgaveController skal fungere`() {
        every {
            oppgaveService.hentOppgaver(any())
        } returns FinnOppgaveResponseDto(1, listOf(Oppgave(tema = Tema.BAR)))
        val response = oppgaveController.hentOppgaver(RestFinnOppgaveRequest())
        val oppgaverOgAntall = response.body?.data as FinnOppgaveResponseDto
        Assertions.assertEquals(1, oppgaverOgAntall.antallTreffTotalt)
        Assertions.assertEquals(Tema.BAR, oppgaverOgAntall.oppgaver.first().tema)
    }
}
