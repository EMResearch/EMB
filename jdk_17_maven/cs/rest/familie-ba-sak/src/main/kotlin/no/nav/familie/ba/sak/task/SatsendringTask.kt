package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = SatsendringTask.TASK_STEP_TYPE,
    beskrivelse = "Utfør satsendring",
    maxAntallFeil = 1,
)
class SatsendringTask(
    val autovedtakSatsendringService: AutovedtakSatsendringService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val dto = objectMapper.readValue(task.payload, SatsendringTaskDto::class.java)

        val resultat = autovedtakSatsendringService.kjørBehandling(dto)

        task.metadata["resultat"] = resultat.melding
    }

    companion object {

        const val TASK_STEP_TYPE = "satsendring"
    }
}

data class SatsendringTaskDto(
    val fagsakId: Long,
    val satstidspunkt: YearMonth,
)
