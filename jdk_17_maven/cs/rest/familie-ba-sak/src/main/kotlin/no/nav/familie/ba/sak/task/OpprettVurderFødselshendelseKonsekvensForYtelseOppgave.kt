package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.AktørId
import no.nav.familie.ba.sak.task.dto.OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettVurderFødselshendelseKonsekvensForYtelseOppgave.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for fødselshendelse som ikke lar seg utføre automatisk",
    maxAntallFeil = 3,
)
class OpprettVurderFødselshendelseKonsekvensForYtelseOppgave(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO = objectMapper.readValue(task.payload, OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO::class.java)
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgaveForFødselshendelse(
            ident = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.ident,
            oppgavetype = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.oppgavetype,
            fristForFerdigstillelse = LocalDate.now(),
            beskrivelse = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.beskrivelse,
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "opprettVurderFødselshendelseKonsekvensForYtelseOppgave"

        fun opprettTask(
            ident: AktørId,
            oppgavetype: Oppgavetype,
            beskrivelse: String,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO(
                        ident,
                        oppgavetype,
                        beskrivelse,
                    ),
                ),
            )
        }
    }
}
