package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskGjenopptaBehandlingBatchTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var automatiskGjenopptaBehandlingBatch: AutomatiskGjenopptaBehandlingBatch

    @Test
    fun `skal lage task på behandling som venter på varsel og tidsfristen har utgått`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().minusWeeks(4),
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        taskService.findAll().any {
            it.type == AutomatiskGjenopptaBehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
        }.shouldBeTrue()
    }

    @Test
    fun `skal lage task på behandling som venter på avvent dokumentasjon`() {
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
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        taskService.findAll().any {
            it.type == AutomatiskGjenopptaBehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
        }.shouldBeTrue()
    }
}
