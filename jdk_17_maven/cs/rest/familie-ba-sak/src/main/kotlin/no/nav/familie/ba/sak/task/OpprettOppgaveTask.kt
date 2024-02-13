package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for behandling",
    maxAntallFeil = 3,
)
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opprettOppgaveTaskDTO = objectMapper.readValue(task.payload, OpprettOppgaveTaskDTO::class.java)
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgave(
            behandlingId = opprettOppgaveTaskDTO.behandlingId,
            oppgavetype = opprettOppgaveTaskDTO.oppgavetype,
            fristForFerdigstillelse = opprettOppgaveTaskDTO.fristForFerdigstillelse,
            tilordnetNavIdent = opprettOppgaveTaskDTO.tilordnetRessurs,
            beskrivelse = opprettOppgaveTaskDTO.beskrivelse,
            manuellOppgaveType = opprettOppgaveTaskDTO.manuellOppgaveType,
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "opprettOppgaveTask"

        fun opprettTask(
            behandlingId: Long,
            oppgavetype: Oppgavetype,
            fristForFerdigstillelse: LocalDate,
            tilordnetRessurs: String? = null,
            beskrivelse: String? = null,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    OpprettOppgaveTaskDTO(
                        behandlingId,
                        oppgavetype,
                        fristForFerdigstillelse,
                        tilordnetRessurs,
                        beskrivelse,
                        null,
                    ),
                ),
            )
        }
    }
}
