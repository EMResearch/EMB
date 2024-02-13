package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class AutobrevScheduler(val taskRepository: TaskRepositoryWrapper) {

    /**
     * Denne funksjonen kjøres kl.7 den første dagen i måneden og setter triggertid på tasken til kl.8 den første virkedagen i måneden.
     * For testformål kan funksjonen opprettTask også kalles direkte via et restendepunkt.
     */
    @Transactional
    @Scheduled(cron = "0 0 $KLOKKETIME_SCHEDULER_TRIGGES 1 * *")
    fun opprettAutobrevTask() {
        when (LeaderClient.isLeader()) {
            true -> {
                // Timen for triggertid økes med en. Det er nødvendig å sette klokkeslettet litt frem dersom den 1. i
                // måneden også er en virkedag (slik at både denne skeduleren og tasken som opprettes vil kjøre på samme dato).
                opprettTask(
                    triggerTid = VirkedagerProvider.nesteVirkedag(
                        LocalDate.now().minusDays(1),
                    ).atTime(KLOKKETIME_SCHEDULER_TRIGGES.inc(), 0),
                )
            }

            false -> logger.info("Poden er ikke satt opp som leader - oppretter ikke task")
            null -> logger.info("Poden svarer ikke om den er leader eller ikke - oppretter ikke task")
        }
    }

    fun opprettTask(triggerTid: LocalDateTime = LocalDateTime.now().plusSeconds(30)) {
        logger.info("Opprett månedlig task")
        taskRepository.save(
            Task(
                type = AutobrevTask.TASK_STEP_TYPE,
                payload = "",
            ).medTriggerTid(
                triggerTid = triggerTid,
            ),
        )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AutobrevScheduler::class.java)
        const val KLOKKETIME_SCHEDULER_TRIGGES = 6
    }
}
