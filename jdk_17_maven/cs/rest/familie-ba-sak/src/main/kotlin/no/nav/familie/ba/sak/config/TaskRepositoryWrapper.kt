package no.nav.familie.ba.sak.config

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/*
TaskRepository in familie-prosessering is @Primary, which is not able to mock so we use this wrapper class for testibility
 */
@Profile("!mock-task-repository")
@Component
class TaskRepositoryWrapper(private val taskService: TaskService) {

    fun save(task: Task) =
        taskService.save(task)

    fun findAll(): Iterable<Task> =
        taskService.findAll()

    fun findByStatus(status: Status): List<Task> =
        taskService.finnTasksMedStatus(listOf(status), type = null, page = Pageable.unpaged())
}
