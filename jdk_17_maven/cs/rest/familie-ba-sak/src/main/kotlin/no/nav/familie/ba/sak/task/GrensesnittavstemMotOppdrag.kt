package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
    beskrivelse = "Grensesnittavstemming mot oppdrag",
    maxAntallFeil = 3,
)
class GrensesnittavstemMotOppdrag(val avstemmingService: AvstemmingService, val taskRepository: TaskRepositoryWrapper) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val avstemmingTask = objectMapper.readValue(task.payload, GrensesnittavstemmingTaskDTO::class.java)
        logger.info("Gjør avstemming mot oppdrag fra og med ${avstemmingTask.fomDato} til og med ${avstemmingTask.tomDato}")

        avstemmingService.grensesnittavstemOppdrag(avstemmingTask.fomDato, avstemmingTask.tomDato)
    }

    override fun onCompletion(task: Task) {
        val nesteAvstemmingTaskDTO = nesteAvstemmingDTO(task.triggerTid.toLocalDate())

        val nesteAvstemmingTask = Task(
            type = TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(nesteAvstemmingTaskDTO),
        ).medTriggerTid(
            nesteAvstemmingTaskDTO.tomDato.toLocalDate().atTime(8, 0),
        )

        taskRepository.save(nesteAvstemmingTask)
    }

    fun nesteAvstemmingDTO(tideligereTriggerDato: LocalDate): GrensesnittavstemmingTaskDTO =
        GrensesnittavstemmingTaskDTO(
            tideligereTriggerDato.atStartOfDay(),
            VirkedagerProvider.nesteVirkedag(tideligereTriggerDato).atStartOfDay(),
        )

    companion object {

        const val TASK_STEP_TYPE = "avstemMotOppdrag"

        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemMotOppdrag::class.java)
    }
}
