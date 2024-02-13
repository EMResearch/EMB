package no.nav.familie.tilbake.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties

internal class OppdaterAnsvarligSaksbehandlerTaskTest {

    private val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    private val fagsakRepository: FagsakRepository = mockk(relaxed = true)
    private val mockOppgaveService: OppgaveService = mockk(relaxed = true)
    private val oppgavePrioritetService = mockk<OppgavePrioritetService>()
    private val behandling: Behandling = Testdata.behandling

    private val oppdaterAnsvarligSaksbehandlerTask =
        OppdaterAnsvarligSaksbehandlerTask(mockOppgaveService, behandlingRepository, oppgavePrioritetService)

    @BeforeEach
    fun init() {
        clearMocks(mockOppgaveService)
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) } returns Testdata.behandling
        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
    }

    @Test
    fun `doTask skal oppdatere oppgave når prioritet endret`() {
        val oppgave = Oppgave(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.HOY
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify {
            mockOppgaveService.patchOppgave(
                oppgave.copy(
                    tilordnetRessurs = behandling.ansvarligSaksbehandler,
                    prioritet = OppgavePrioritet.HOY,
                ),
            )
        }
    }

    @Test
    fun `doTask skal oppdatere oppgave når saksbehandler endret`() {
        val oppgave = Oppgave(tilordnetRessurs = "TIDLIGERE saksbehandler", prioritet = OppgavePrioritet.NORM)
        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify(atLeast = 1) {
            mockOppgaveService.patchOppgave(
                oppgave.copy(
                    tilordnetRessurs = behandling.ansvarligSaksbehandler,
                    prioritet = OppgavePrioritet.NORM,
                ),
            )
        }
    }

    @Test
    fun `Skal ikke oppdatere oppgave når ingenting er endret`() {
        val oppgave = Oppgave(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify(exactly = 0) { mockOppgaveService.patchOppgave(any()) }
    }

    private fun lagTask(opprettetAv: String? = null): Task {
        return Task(
            type = OppdaterAnsvarligSaksbehandlerTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                setProperty("oppgavetype", Oppgavetype.BehandleSak.name)
                setProperty(PropertyName.ENHET, "enhet")
                if (opprettetAv != null) {
                    setProperty("opprettetAv", opprettetAv)
                }
            },
        )
    }
}
