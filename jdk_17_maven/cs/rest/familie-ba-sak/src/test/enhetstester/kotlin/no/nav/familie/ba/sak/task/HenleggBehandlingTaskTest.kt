package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HenleggBehandlingTaskTest {

    val oppgaveService: OppgaveService = mockk()
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    val stegService: StegService = mockk(relaxed = true)
    private val henleggBehandlingTask = HenleggBehandlingTask(
        arbeidsfordelingService = mockk(relaxed = true),
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        stegService = stegService,
        oppgaveService = oppgaveService,
    )

    @Test
    fun `skal ikke henlegge dersom behandlingen allerede er avsluttet`() {
        every { behandlingHentOgPersisterService.hent(any()) } returns lagBehandling(status = BehandlingStatus.AVSLUTTET)
        val task = opprettTekniskHenleggelseGrunnetSatsendringTask()
        henleggBehandlingTask.doTask(task)
        verify(exactly = 0) {
            stegService.håndterHenleggBehandling(any(), any())
        }
        assertThat(task.metadata["Resultat"]).isEqualTo("Behandlingen er allerede avsluttet")
    }

    @Test
    fun `skal ikke henlegge dersom behandlingsfristen ikke er etter angitt valideringsdato`() {
        val behandling = lagBehandling()
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(any(), any()) } returns
            listOf(DbOppgave(behandling = behandling, gsakId = "1", type = Oppgavetype.BehandleSak))
        every { oppgaveService.hentOppgave(any()) } returns lagTestOppgaveDTO(1, Oppgavetype.BehandleSak)

        val task = opprettTekniskHenleggelseGrunnetSatsendringTask()
        henleggBehandlingTask.doTask(task)
        verify(exactly = 0) {
            stegService.håndterHenleggBehandling(any(), any())
        }
        assertThat(task.metadata["Resultat"] as String).contains("frist", "Må være etter 2023-04-01")
    }

    @Test
    fun `skal henlegge med årsak TEKNISK_VEDLIKEHOLD og begrunnelse Satsendring`() {
        val behandling = lagBehandling()
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(any(), any()) } returns
            listOf(DbOppgave(behandling = behandling, gsakId = "1", type = Oppgavetype.BehandleSak))
        every { oppgaveService.hentOppgave(any()) } returns lagTestOppgaveDTO(1, Oppgavetype.BehandleSak).copy(
            fristFerdigstillelse = LocalDate.of(2023, 4, 2).toString(),
        )

        val task = opprettTekniskHenleggelseGrunnetSatsendringTask()
        henleggBehandlingTask.doTask(task)

        val henleggBehandlingInfo = slot<RestHenleggBehandlingInfo>()
        verify(exactly = 1) {
            stegService.håndterHenleggBehandling(behandling, capture(henleggBehandlingInfo))
        }
        assertThat(henleggBehandlingInfo.captured.årsak).isEqualTo(HenleggÅrsak.TEKNISK_VEDLIKEHOLD)
        assertThat(henleggBehandlingInfo.captured.begrunnelse).isEqualTo("Satsendring")
        assertThat(task.metadata["Resultat"]).isEqualTo("Henleggelse kjørt OK")
    }

    private fun opprettTekniskHenleggelseGrunnetSatsendringTask(): Task {
        return Task(
            type = HenleggBehandlingTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                HenleggBehandlingTaskDTO(
                    behandlingId = 1,
                    årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                    begrunnelse = "Satsendring",
                    validerOppgavefristErEtterDato = LocalDate.of(2023, 4, 1),
                ),
            ),
        )
    }
}
