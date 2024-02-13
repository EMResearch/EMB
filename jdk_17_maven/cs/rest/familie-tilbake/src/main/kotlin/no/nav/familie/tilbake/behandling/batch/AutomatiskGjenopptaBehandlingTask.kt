package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskGjenopptaBehandlingTask.TYPE,
    beskrivelse = "gjenopptar behandling automatisk",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AutomatiskGjenopptaBehandlingTask(
    private val automatiskGjenopptaBehandlingService: AutomatiskGjenopptaBehandlingService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("AutomatiskGjenopptaBehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        automatiskGjenopptaBehandlingService.gjenopptaBehandling(behandlingId)
    }

    companion object {

        const val TYPE = "gjenoppta.behandling.automatisk"
    }
}
