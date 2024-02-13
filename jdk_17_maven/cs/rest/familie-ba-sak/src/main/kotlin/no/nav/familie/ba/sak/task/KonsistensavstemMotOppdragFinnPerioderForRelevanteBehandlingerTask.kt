package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
    beskrivelse = "Finn perioder til avstemming for relevante behandlinger",
    maxAntallFeil = 3,
)
class KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask(
    val avstemmingService: AvstemmingService,
    val taskService: TaskService,
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskDto =
            objectMapper.readValue(
                task.payload,
                KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO::class.java,
            )

        if (avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(
                taskDto.transaksjonsId,
                taskDto.chunkNr,
            )
        ) {
            logger.info("Finn perioder for avstemming er alt kjørt for ${taskDto.transaksjonsId} og ${taskDto.chunkNr}")
            return
        }

        val perioderTilAvstemming =
            avstemmingService.hentDataForKonsistensavstemming(
                taskDto.avstemmingsdato,
                taskDto.relevanteBehandlinger,
            )

        logger.info("Finner perioder til avstemming for transaksjonsId ${taskDto.transaksjonsId} og chunk ${taskDto.chunkNr} med ${perioderTilAvstemming.size} løpende saker")
        val konsistensavstemmingDataTask = Task(
            type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                KonsistensavstemmingDataTaskDTO(
                    transaksjonsId = taskDto.transaksjonsId,
                    chunkNr = taskDto.chunkNr,
                    avstemmingdato = taskDto.avstemmingsdato,
                    perioderForBehandling = perioderTilAvstemming,
                    sendTilØkonomi = taskDto.sendTilØkonomi,
                ),
            ),
            properties = Properties().apply {
                this["chunkNr"] = taskDto.chunkNr.toString()
                this["transaksjonsId"] = taskDto.transaksjonsId.toString()
            },
        )
        taskService.save(konsistensavstemmingDataTask)
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask::class.java)

        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlinger"
    }
}
