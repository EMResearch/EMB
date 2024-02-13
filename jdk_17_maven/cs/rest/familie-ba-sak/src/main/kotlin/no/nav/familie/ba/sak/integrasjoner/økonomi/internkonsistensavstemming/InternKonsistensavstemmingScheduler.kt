package no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming

import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InternKonsistensavstemmingScheduler(
    val taskService: TaskService,
) {

    @Scheduled(cron = "0 0 0 29 * *")
    fun startInternKonsistensavstemming() {
        if (LeaderClient.isLeader() == true) {
            taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask())
        }
    }
}
