package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterAnsvarligSaksbehandlerTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer saksbehandler på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterAnsvarligSaksbehandlerTask(
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgavePrioritetService: OppgavePrioritetService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterSaksbehandlerPåOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        if (oppgave.tilordnetRessurs != behandling.ansvarligSaksbehandler || oppgave.prioritet != prioritet) {
            oppgaveService.patchOppgave(oppgave.copy(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = prioritet))
        }
    }

    companion object {

        const val TYPE = "oppdaterSaksbehandlerOppgave"
    }
}
