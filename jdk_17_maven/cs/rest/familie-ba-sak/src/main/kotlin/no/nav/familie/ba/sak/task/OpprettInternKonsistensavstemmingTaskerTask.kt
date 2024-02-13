package no.nav.familie.ba.sak.task.internkonsistensavstemming

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming.InternKonsistensavstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettInternKonsistensavstemmingTaskerTask.TASK_STEP_TYPE,
    beskrivelse = "Start intern konsistensavstemming tasker",
    maxAntallFeil = 3,
)
class OpprettInternKonsistensavstemmingTaskerTask(
    val internKonsistensavstemmingService: InternKonsistensavstemmingService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val maksAntallTasker: Int = objectMapper.readValue(task.payload)
        internKonsistensavstemmingService
            .validerLikUtbetalingIAndeleneOgUtbetalingsoppdragetPåAlleFagsaker(maksAntallTasker)
    }

    companion object {
        fun opprettTask(maksAntallTasker: Int = Int.MAX_VALUE): Task {
            val metadata = Properties().apply {
                this[MDCConstants.MDC_CALL_ID] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
            }

            return Task(
                type = TASK_STEP_TYPE,
                payload = maksAntallTasker.toString(),
                triggerTid = LocalDateTime.now(),
                metadataWrapper = PropertiesWrapper(metadata),
            )
        }

        const val TASK_STEP_TYPE = "startInternKonsistensavstemmingTasker"
    }
}
