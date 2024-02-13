package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrevTask(
    private val stegService: StegService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val brevmalService: BrevmalService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerVedtaksbrevDTO = objectMapper.readValue(task.payload, DistribuerVedtaksbrevDTO::class.java)

        val behandling = behandlingHentOgPersisterService.hent(distribuerVedtaksbrevDTO.behandlingId)

        val distribuerDokumentDTO = DistribuerDokumentDTO(
            behandlingId = distribuerVedtaksbrevDTO.behandlingId,
            journalpostId = distribuerVedtaksbrevDTO.journalpostId,
            personEllerInstitusjonIdent = distribuerVedtaksbrevDTO.personIdent,
            brevmal = brevmalService.hentBrevmal(behandling),
            erManueltSendt = false,
        )
        stegService.håndterDistribuerVedtaksbrev(
            behandling = behandlingHentOgPersisterService.hent(distribuerVedtaksbrevDTO.behandlingId),
            distribuerDokumentDTO = distribuerDokumentDTO,
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
    }
}

data class DistribuerVedtaksbrevDTO(
    val behandlingId: Long,
    val journalpostId: String,
    val personIdent: String,
)
