package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

internal class AutomatiskGjenopptaBehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var automatiskGjenopptaBehandlingTask: AutomatiskGjenopptaBehandlingTask

    @Test
    fun `skal gjenoppta behandling som venter på varsel og har allerede fått kravgrunnlag til FAKTA steg`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        val tidsfrist = LocalDate.now().minusWeeks(4)
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = tidsfrist,
            ),
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingTask.doTask(lagTask(behandling.id)) }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }.shouldBeTrue()
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.FAKTA &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent automatisk" &&
                it.metadata["frist"] == tidsfrist.toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()
    }

    @Test
    fun `skal gjenoppta behandling som venter på varsel og har ikke fått kravgrunnlag til GRUNNLAG steg`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        val tidsfrist = LocalDate.now().minusWeeks(4)
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = tidsfrist,
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingTask.doTask(lagTask(behandling.id)) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }.shouldBeTrue()
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.GRUNNLAG &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER &&
                it.venteårsak == Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent automatisk" &&
                it.metadata["frist"] == tidsfrist.toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()

        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        taskService.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == venteårsak.beskrivelse
            it.metadata["frist"] == LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker).toString()
        }.shouldBeTrue()
    }

    @Test
    fun `skal gjenoppta behandling som venter på avvent dokumentasjon`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        val tidsfrist = LocalDate.now().minusWeeks(1)
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
                tidsfrist = tidsfrist,
            ),
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingTask.doTask(lagTask(behandling.id)) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
            it.venteårsak == null && it.tidsfrist == null
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskService.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent automatisk" &&
                it.metadata["frist"] == tidsfrist.toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()
    }

    private fun lagTask(behandlingId: UUID) = Task(
        type = AutomatiskGjenopptaBehandlingTask.TYPE,
        payload = behandlingId.toString(),
        Properties().apply {
            setProperty(
                PropertyName.FAGSYSTEM,
                Fagsystem.BA.name,
            )
        },
    )
}
