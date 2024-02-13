package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.TaBehandlingerEtterVentefristAvVentTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SettPåVentScheduler(val taskRepository: TaskRepositoryWrapper) {

    @Scheduled(cron = "0 0 7 * * *")
    fun taBehandlingerEtterVentefristAvVent() {
        when (LeaderClient.isLeader()) {
            true -> {
                val taBehandlingerEtterVentefristAvVentTask =
                    Task(type = TaBehandlingerEtterVentefristAvVentTask.TASK_STEP_TYPE, payload = "")
                taskRepository.save(taBehandlingerEtterVentefristAvVentTask)
                logger.info("Opprettet taBehandlingerAvVentTask")
            }
            false, null -> {
                logger.info("Ikke opprettet taBehandlingerAvVentTask på denne poden")
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SettPåVentScheduler::class.java)
    }
}
