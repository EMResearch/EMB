package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragStartTask
        .TASK_STEP_TYPE,
    beskrivelse = "Start Konsistensavstemming mot oppdrag",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class KonsistensavstemMotOppdragStartTask(val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingStartTaskDTO::class.java)

        val avstemmingsdato = LocalDateTime.now()
        logger.info("Konsistensavstemming ble initielt trigget ${konsistensavstemmingTask.avstemmingdato}, men bruker $avstemmingsdato som avstemmingsdato")

        if (avstemmingService.harBatchStatusFerdig(konsistensavstemmingTask.batchId)) {
            logger.info("Konsistensavstemmning er allerede kjørt for transaksjonsId=${konsistensavstemmingTask.transaksjonsId} og batchId=${konsistensavstemmingTask.batchId}")
            return
        }

        if (!avstemmingService.erKonsistensavstemmingStartet(konsistensavstemmingTask.transaksjonsId)) {
            if (konsistensavstemmingTask.sendTilØkonomi) {
                avstemmingService.sendKonsistensavstemmingStart(
                    avstemmingsdato,
                    konsistensavstemmingTask.transaksjonsId,
                )
            } else {
                logger.info("Send startmelding til økonomi i dry-run modus for ${konsistensavstemmingTask.transaksjonsId}")
            }
        }

        val relevanteBehandlinger =
            avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker().toSet().sorted()

        var chunkNr = 1
        val startTid = LocalDateTime.now()
        relevanteBehandlinger.chunked(AvstemmingService.KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK)
            .forEachIndexed { index, oppstykketRelevanteBehandlinger ->
                val triggerTidForChunk = startTid.plusSeconds(3 * index.toLong())
                if (avstemmingService.skalOppretteFinnPerioderForRelevanteBehandlingerTask(
                        konsistensavstemmingTask.transaksjonsId,
                        chunkNr,
                    )
                ) {
                    avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(
                        KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
                            transaksjonsId = konsistensavstemmingTask.transaksjonsId,
                            chunkNr = chunkNr,
                            avstemmingsdato = avstemmingsdato,
                            batchId = konsistensavstemmingTask.batchId,
                            relevanteBehandlinger = oppstykketRelevanteBehandlinger.map { it },
                            sendTilØkonomi = konsistensavstemmingTask.sendTilØkonomi,
                        ),
                        triggerTidForChunk,
                    )
                } else {
                    logger.info("Finn perioder for avstemming task alt kjørt for ${konsistensavstemmingTask.transaksjonsId} og chunkNr $chunkNr")
                }
                chunkNr = chunkNr.inc()
            }

        avstemmingService.opprettKonsistensavstemmingAvsluttTask(
            KonsistensavstemmingAvsluttTaskDTO(
                batchId = konsistensavstemmingTask.batchId,
                transaksjonsId = konsistensavstemmingTask.transaksjonsId,
                avstemmingsdato = avstemmingsdato,
                sendTilØkonomi = konsistensavstemmingTask.sendTilØkonomi,
            ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragStart"
        private val logger = LoggerFactory.getLogger(KonsistensavstemMotOppdragStartTask::class.java)
    }
}
