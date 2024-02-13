package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.IverksettMotFamilieTilbakeDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = IverksettMotFamilieTilbakeTask.TASK_STEP_TYPE,
    beskrivelse = "Iverksett mot Familie tilbake",
    maxAntallFeil = 3,
)
class IverksettMotFamilieTilbakeTask(
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val stegService: StegService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val iverksettMotFamilieTilbake = objectMapper.readValue(task.payload, IverksettMotFamilieTilbakeDTO::class.java)
        stegService.håndterIverksettMotFamilieTilbake(
            behandling = behandlingHentOgPersisterService.hent(iverksettMotFamilieTilbake.behandlingsId),
            task.metadata,
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "iverksettMotFamilieTilbake"
        fun opprettTask(behandlingsId: Long, metadata: Properties): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(IverksettMotFamilieTilbakeDTO(behandlingsId)),
                properties = metadata.apply {
                    this["behandlingId"] = behandlingsId.toString()
                },
            )
        }
    }
}
