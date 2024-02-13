package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterPrioritetTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer prioritet på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterPrioritetTask(
    private val oppgaveService: OppgaveService,
    private val oppgavePrioritetService: OppgavePrioritetService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterPrioritetTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        oppgaveService.patchOppgave(oppgave.copy(prioritet = prioritet))
    }

    companion object {

        const val TYPE = "oppdaterPrioritetForOppgave"
    }
}
