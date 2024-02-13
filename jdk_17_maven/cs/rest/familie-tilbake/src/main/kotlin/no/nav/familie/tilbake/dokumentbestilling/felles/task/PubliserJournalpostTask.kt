package no.nav.familie.tilbake.dokumentbestilling.felles.task

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PubliserJournalpostTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Publiserer journalpost",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class PubliserJournalpostTask(
    private val integrasjonerClient: IntegrasjonerClient,
    private val taskService: TaskService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")

        val journalpostId = task.metadata.getProperty("journalpostId")
        val (behandlingId, manuellAdresse) = objectMapper.readValue(task.payload, PubliserJournalpostTaskData::class.java)
            .let { it.behandlingId to it.manuellAdresse }

        prøvDistribuerJournalpost(journalpostId, task, behandlingId, manuellAdresse)
    }

    private fun prøvDistribuerJournalpost(
        journalpostId: String,
        task: Task,
        behandlingId: UUID,
        manuellAdresse: ManuellAdresse? = null,
    ) {
        try {
            integrasjonerClient.distribuerJournalpost(
                journalpostId,
                Fagsystem.valueOf(task.metadata.getProperty("fagsystem")),
                Distribusjonstype.valueOf(task.metadata.getProperty("distribusjonstype")),
                Distribusjonstidspunkt.valueOf(task.metadata.getProperty("distribusjonstidspunkt")),
                manuellAdresse,
            )
        } catch (ressursException: RessursException) {
            when {
                mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) -> {
                    // ta med info om ukjent adresse
                    task.metadata["ukjentAdresse"] = "true"
                }

                DistribuerDokumentVedDødsfallTask.mottakerErDødUtenDødsboadresse(ressursException) -> {
                    // ta med info om ukjent adresse for dødsbo
                    task.metadata["dødsboUkjentAdresse"] = "true"
                    taskService.save(Task(DistribuerDokumentVedDødsfallTask.TYPE, behandlingId.toString(), task.metadata))
                }

                dokumentetErAlleredeDistribuert(ressursException) -> {
                    log.warn(
                        "Journalpost med Id=$journalpostId er allerede distiribuert. Hopper over distribuering. BehandlingId=$behandlingId.",
                    )
                }

                else -> throw ressursException
            }
        }
    }

    override fun onCompletion(task: Task) {
        val behandlingId = objectMapper.readValue(task.payload, PubliserJournalpostTaskData::class.java).behandlingId
        taskService.save(Task(LagreBrevsporingTask.TYPE, behandlingId.toString(), task.metadata))
    }

    // 400 BAD_REQUEST + kanal print er eneste måten å vite at bruker ikke er digital og har ukjent adresse fra Dokdist
    // https://nav-it.slack.com/archives/C6W9E5GPJ/p1647947002270879?thread_ts=1647936835.099329&cid=C6W9E5GPJ
    fun mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException: RessursException) =
        ressursException.httpStatus == HttpStatus.BAD_REQUEST &&
            ressursException.cause?.message?.contains("Mottaker har ukjent adresse") == true

    // 409 Conflict betyr duplikatdistribusjon
    // https://nav-it.slack.com/archives/C6W9E5GPJ/p1657610907144549?thread_ts=1657610829.116619&cid=C6W9E5GPJ
    fun dokumentetErAlleredeDistribuert(ressursException: RessursException) =
        ressursException.httpStatus == HttpStatus.CONFLICT

    companion object {

        const val TYPE = "publiserJournalpost"
    }
}

class PubliserJournalpostTaskData(
    val behandlingId: UUID,
    val manuellAdresse: ManuellAdresse?,
)
