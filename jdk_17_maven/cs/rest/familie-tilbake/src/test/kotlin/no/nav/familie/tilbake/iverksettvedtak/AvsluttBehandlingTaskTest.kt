package no.nav.familie.tilbake.iverksettvedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AvsluttBehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var avsluttBehandlingTask: AvsluttBehandlingTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal avslutte behandling`() {
        var behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.IVERKSETTER_VEDTAK))
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandlingId,
                behandlingssteg = Behandlingssteg.AVSLUTTET,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
        )

        avsluttBehandlingTask.doTask(Task(type = AvsluttBehandlingTask.TYPE, payload = behandlingId.toString()))

        behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET

        val stegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        stegstilstand.size shouldBe 1
        stegstilstand[0].behandlingssteg shouldBe Behandlingssteg.AVSLUTTET
        stegstilstand[0].behandlingsstegsstatus shouldBe Behandlingsstegstatus.UTFØRT

        val tasker = taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
        val historikkTask = tasker.first { it.type == LagHistorikkinnslagTask.TYPE }
        historikkTask.type shouldBe LagHistorikkinnslagTask.TYPE
        historikkTask.payload shouldBe behandlingId.toString()
        val taskProperty = historikkTask.metadata
        taskProperty["aktør"] shouldBe Aktør.VEDTAKSLØSNING.name
        taskProperty["historikkinnslagstype"] shouldBe TilbakekrevingHistorikkinnslagstype.BEHANDLING_AVSLUTTET.name
    }
}
