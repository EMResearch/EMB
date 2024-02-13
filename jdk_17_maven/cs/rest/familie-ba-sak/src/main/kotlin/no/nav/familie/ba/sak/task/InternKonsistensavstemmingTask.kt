package no.nav.familie.ba.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming.InternKonsistensavstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import kotlin.system.measureTimeMillis

@Service
@TaskStepBeskrivelse(
    taskStepType = InternKonsistensavstemmingTask.TASK_STEP_TYPE,
    beskrivelse = "Kjør intern konsistensavstemming",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 600,
)
class InternKonsistensavstemmingTask(
    val internKonsistensavstemmingService: InternKonsistensavstemmingService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val fagsakIder: Set<Long> = objectMapper.readValue(task.payload)

        val tidBrukt = measureTimeMillis {
            internKonsistensavstemmingService.validerLikUtbetalingIAndeleneOgUtbetalingsoppdraget(fagsakIder)
        }

        logger.info(
            "Fullført intern konsistensavstemming på fagsak ${fagsakIder.min()} til ${fagsakIder.max()}. " +
                "Tid brukt = $tidBrukt millisekunder",
        )
    }

    companion object {
        fun opprettTask(fagsakIder: Set<Long>, startTid: LocalDateTime): Task {
            val metadata = Properties().apply {
                this["fagsakerIder"] = "${fagsakIder.min()} til ${fagsakIder.max()}"
                this[MDCConstants.MDC_CALL_ID] = MDC.get(MDCConstants.MDC_CALL_ID) ?: ""
            }

            return Task(
                type = this.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(fagsakIder),
                triggerTid = startTid,
                metadataWrapper = PropertiesWrapper(metadata),
            )
        }

        const val TASK_STEP_TYPE = "internKonsistensavstemming"
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
