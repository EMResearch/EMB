package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentFagsystemsbehandlingTask.TYPE,
    beskrivelse = "Sender kafka request til fagsystem for å hente behandling data",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class HentFagsystemsbehandlingTask(
    private val håndterGamleKravgrunnlagService: HåndterGamleKravgrunnlagService,
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val taskService: TaskService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("HentFagsystemsbehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val mottattXmlId = UUID.fromString(task.payload)
        val mottattXml = håndterGamleKravgrunnlagService.hentFrakobletKravgrunnlag(mottattXmlId)
        task.metadata["eksternFagsakId"] = mottattXml.eksternFagsakId

        håndterGamleKravgrunnlagService.sjekkOmDetFinnesEnAktivBehandling(mottattXml)
        hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(
            eksternFagsakId = mottattXml.eksternFagsakId,
            ytelsestype = mottattXml.ytelsestype,
            eksternId = mottattXml.referanse,
        )
    }

    @Transactional
    override fun onCompletion(task: Task) {
        logger.info("Oppretter HåndterGammelKravgrunnlagTask for mottattXmlId=${task.payload}")
        taskService.save(
            Task(
                type = HåndterGammelKravgrunnlagTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ).medTriggerTid(LocalDateTime.now().plusSeconds(60)),
        )
    }

    companion object {

        const val TYPE = "gammelKravgrunnlag.hentFagsystemsbehandling"
    }
}
