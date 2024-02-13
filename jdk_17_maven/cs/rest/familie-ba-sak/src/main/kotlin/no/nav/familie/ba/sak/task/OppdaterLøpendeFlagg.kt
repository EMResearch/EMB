package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterLøpendeFlagg.TASK_STEP_TYPE,
    beskrivelse = "Oppdater fagsakstatus fra LØPENDE til AVSLUTTET på avsluttede fagsaker",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60,
)
class OppdaterLøpendeFlagg(val fagsakService: FagsakService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val antallOppdaterte = fagsakService.oppdaterLøpendeStatusPåFagsaker()
        logger.info("Oppdatert status på $antallOppdaterte fagsaker til ${FagsakStatus.AVSLUTTET.name}")
    }

    companion object {

        const val TASK_STEP_TYPE = "oppdaterLøpendeFlagg"
        private val logger: Logger = LoggerFactory.getLogger(OppdaterLøpendeFlagg::class.java)
    }
}
