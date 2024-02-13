package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør brev i Joark", maxAntallFeil = 3)
class JournalførVedtaksbrevTask(
    private val vedtakService: VedtakService,
    private val stegService: StegService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakId = task.payload.toLong()
        val behandling = vedtakService.hent(vedtakId).behandling

        stegService.håndterJournalførVedtaksbrev(behandling, JournalførVedtaksbrevDTO(vedtakId = vedtakId, task = task))
    }

    companion object {

        const val TASK_STEP_TYPE = "journalførTilJoark"

        fun opprettTaskJournalførVedtaksbrev(
            personIdent: String,
            behandlingId: Long,
            vedtakId: Long,
            gammelTask: Task? = null,
        ): Task {
            return Task(
                TASK_STEP_TYPE,
                "$vedtakId",
                gammelTask?.metadata ?: Properties().apply {
                    this["personIdent"] = personIdent
                    this["behandlingsId"] = behandlingId.toString()
                    this["vedtakId"] = vedtakId.toString()
                },
            )
        }
    }
}
