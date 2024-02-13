package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.FerdigstillOppgaveDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaver.TASK_STEP_TYPE,
    beskrivelse = "Ferdigstill oppgaver i GOSYS for behandling",
    maxAntallFeil = 3,
)
class FerdigstillOppgaver(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillOppgave = objectMapper.readValue(task.payload, FerdigstillOppgaveDTO::class.java)
        oppgaveService.ferdigstillOppgaver(
            behandlingId = ferdigstillOppgave.behandlingId,
            oppgavetype = ferdigstillOppgave.oppgavetype,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillOppgaveTask"

        fun opprettTask(behandlingId: Long, oppgavetype: Oppgavetype): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    FerdigstillOppgaveDTO(
                        behandlingId = behandlingId,
                        oppgavetype = oppgavetype,
                    ),
                ),
            )
        }
    }
}
