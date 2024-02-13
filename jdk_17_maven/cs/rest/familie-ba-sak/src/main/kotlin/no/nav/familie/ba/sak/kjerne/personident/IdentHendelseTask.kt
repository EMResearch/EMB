package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = IdentHendelseTask.TASK_STEP_TYPE,
    beskrivelse = "Sjekker om ident-hendelse berører person registert i BA-sak og oppdaterer",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong(),
)
class IdentHendelseTask(
    private val personidentService: PersonidentService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        logger.info("Kjører task for håntering av identhendelse.")
        val personIdent = objectMapper.readValue(task.payload, PersonIdent::class.java)
        personidentService.håndterNyIdent(personIdent)
    }

    companion object {
        const val TASK_STEP_TYPE = "IdentHendelseTask"
        private val logger: Logger = LoggerFactory.getLogger(IdentHendelseTask::class.java)

        fun opprettTask(nyIdent: PersonIdent): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(nyIdent),
                properties = Properties().apply {
                    this["nyPersonIdent"] = nyIdent.ident
                },
            ).medTriggerTid(
                triggerTid = LocalDateTime.now().plusMinutes(1),
            )
        }
    }
}
