package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kravgrunnlag.KravvedtakstatusService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleStatusmeldingTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Håndter mottatt statusmelding fra oppdrag",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class BehandleStatusmeldingTask(private val kravvedtakstatusService: KravvedtakstatusService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        log.info("BehandleStatusmeldingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        secureLog.info("BehandleStatusmeldingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        kravvedtakstatusService.håndterMottattStatusmelding(task.payload)
    }

    companion object {

        const val TYPE = "behandleStatusmelding"
    }
}
