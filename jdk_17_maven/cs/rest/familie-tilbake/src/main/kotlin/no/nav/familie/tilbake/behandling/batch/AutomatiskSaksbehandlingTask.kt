package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskSaksbehandlingTask.TYPE,
    beskrivelse = "behandler behandling automatisk",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AutomatiskSaksbehandlingTask(private val automatiskSaksbehandlingService: AutomatiskSaksbehandlingService) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("AutomatiskSaksbehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        automatiskSaksbehandlingService.oppdaterBehandling(behandlingId)

        automatiskSaksbehandlingService.behandleAutomatisk(behandlingId)
    }

    companion object {

        const val TYPE = "saksbehandle.automatisk"
    }
}
