package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = TaBehandlingerEtterVentefristAvVentTask.TASK_STEP_TYPE,
    beskrivelse = "Gjennopptar behandlinger der ventefristen har gått",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60,
)
class TaBehandlingerEtterVentefristAvVentTask(val settPåVentService: SettPåVentService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val sakerPåVent = settPåVentService.finnAktiveSettPåVent()

        sakerPåVent.forEach {
            if (it.frist.isBefore(LocalDate.now())) {
                logger.info("Ventefrist på behandling ${it.behandling.id} er gått ut. Tar behandlingen av vent.")
                settPåVentService.gjenopptaBehandling(behandlingId = it.behandling.id)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaBehandlingerEtterVentefristAvVentTask::class.java)
        const val TASK_STEP_TYPE = "taBehanldingerEtterVentefristenAvVent"
    }
}
