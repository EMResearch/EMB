package no.nav.familie.tilbake.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties

internal class LagOppgaveTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val mockOppgaveService: OppgaveService = mockk(relaxed = true)
    private val mockIntegrasjonerClient = mockk<IntegrasjonerClient>(relaxed = true)
    private val oppgavePrioritetService = mockk<OppgavePrioritetService>()

    private lateinit var lagOppgaveTask: LagOppgaveTask

    private val behandling: Behandling = Testdata.behandling

    private val dagensDato = LocalDate.now()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM

        lagOppgaveTask = LagOppgaveTask(mockOppgaveService, behandlingskontrollService, oppgavePrioritetService)
    }

    @Test
    fun `doTask skal lage oppgave når behandling venter på varsel steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.VENTER, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
        val fristForFerdigstillelse = dagensDato.plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)

        lagOppgaveTask.doTask(lagTask())

        verify {
            mockOppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                "enhet",
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.beskrivelse,
                fristForFerdigstillelse,
                null,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    fun `doTask skal lage oppgave når behandling venter på grunnlag steg`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
        val fristForFerdigstillelse = dagensDato.plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker)

        lagOppgaveTask.doTask(lagTask())

        verify {
            mockOppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                "enhet",
                Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
                fristForFerdigstillelse,
                null,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    fun `doTask skal lage oppgave når behandling er på FAKTA steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        lagOppgaveTask.doTask(lagTask())

        verify {
            mockOppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                enhet = "enhet",
                beskrivelse = "",
                fristForFerdigstillelse = dagensDato,
                saksbehandler = null,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    fun `doTask skal lage oppgave med saksbehandler som sendte til beslutter i beskrivelse`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val opprettetAv = "Saksbehandler Saksbehandlersen"
        lagOppgaveTask.doTask(lagTask(opprettetAv))

        verify {
            mockOppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                enhet = "enhet",
                beskrivelse = "Sendt til godkjenning av Saksbehandler Saksbehandlersen ",
                fristForFerdigstillelse = dagensDato,
                saksbehandler = null,
                OppgavePrioritet.NORM,
            )
        }
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegsstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegsstatus,
                venteårsak = venteårsak,
                tidsfrist = venteårsak?.let {
                    dagensDato.plusWeeks(it.defaultVenteTidIUker)
                },
            ),
        )
    }

    private fun lagTask(opprettetAv: String? = null): Task {
        return Task(
            type = LagOppgaveTask.TYPE,
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
