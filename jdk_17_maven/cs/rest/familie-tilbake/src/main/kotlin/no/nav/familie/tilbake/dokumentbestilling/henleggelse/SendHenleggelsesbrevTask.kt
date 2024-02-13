package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Component
@TaskStepBeskrivelse(
    taskStepType = SendHenleggelsesbrevTask.TYPE,
    maxAntallFeil = 50,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Send henleggelsesbrev.",
)
class SendHenleggelsesbrevTask(
    private val henleggelsesbrevService: HenleggelsesbrevService,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskdata: SendBrevTaskdata = objectMapper.readValue(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(taskdata.behandlingId)
        henleggelsesbrevService.sendHenleggelsebrev(behandling.id, taskdata.fritekst)
    }

    companion object {

        fun opprettTask(
            behandlingId: UUID,
            fagsystem: Fagsystem,
            fritekst: String?,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(SendBrevTaskdata(behandlingId, fritekst)),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            )
                .medTriggerTid(LocalDateTime.now().plusSeconds(15))

        const val TYPE = "distribuerHenleggelsesbrev"
    }
}

data class SendBrevTaskdata(
    val behandlingId: UUID,
    val fritekst: String?,
)
