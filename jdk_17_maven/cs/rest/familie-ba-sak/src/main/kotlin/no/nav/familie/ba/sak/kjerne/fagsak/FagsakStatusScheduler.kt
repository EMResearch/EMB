package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(
    private val taskRepository: TaskRepositoryWrapper,
    private val envService: EnvService,
) {

    /*
     * Siden barnetrygd er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "\${CRON_FAGSAKSTATUS_SCHEDULER}")
    fun oppdaterFagsakStatuser() {
        when (LeaderClient.isLeader() == true || envService.erDev()) {
            true -> {
                val oppdaterLøpendeFlaggTask = Task(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
                taskRepository.save(oppdaterLøpendeFlaggTask)
                logger.info("Opprettet oppdaterLøpendeFlaggTask")
            }
            false -> {
                logger.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}
