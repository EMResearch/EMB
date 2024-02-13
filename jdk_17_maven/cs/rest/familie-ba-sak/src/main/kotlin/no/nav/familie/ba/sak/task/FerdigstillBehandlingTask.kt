package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Ferdigstill behandling",
    maxAntallFeil = 3,
)
class FerdigstillBehandlingTask(
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val stegService: StegService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillBehandling = objectMapper.readValue(task.payload, FerdigstillBehandlingDTO::class.java)
        stegService.håndterFerdigstillBehandling(
            behandling = behandlingHentOgPersisterService.hent(
                ferdigstillBehandling.behandlingsId,
            ),
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "ferdigstillBehandling"

        fun opprettTask(søkerIdent: String, behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    FerdigstillBehandlingDTO(
                        personIdent = søkerIdent,
                        behandlingsId = behandlingsId,
                    ),
                ),
                properties = Properties().apply {
                    this["personIdent"] = søkerIdent
                    this["behandlingsId"] = behandlingsId.toString()
                },
            )
        }
    }
}
