package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.common.exceptionhandler.UkjentravgrunnlagFeil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HåndterGammelKravgrunnlagTask.TYPE,
    beskrivelse = "Håndter frakoblet gammel kravgrunnlag som er eldre enn en bestemt dato",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class HåndterGammelKravgrunnlagTask(
    private val håndterGamleKravgrunnlagService: HåndterGamleKravgrunnlagService,
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
) :
    AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun doTask(task: Task) {
        logger.info("HåndterGammelKravgrunnlagTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val mottattXmlId = UUID.fromString(task.payload)
        val mottattXml = håndterGamleKravgrunnlagService.hentFrakobletKravgrunnlag(mottattXmlId)
        val eksternFagsakId = mottattXml.eksternFagsakId
        val ytelsestype = mottattXml.ytelsestype
        val eksternId = mottattXml.referanse

        val requestSendt = requireNotNull(
            hentFagsystemsbehandlingService.hentFagsystemsbehandlingRequestSendt(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            ),
        )
        // kaster exception inntil respons-en har mottatt
        val respons = requireNotNull(requestSendt.respons) {
            "HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
                "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype,eksternId=$eksternId." +
                "Task-en kan kjøre på nytt manuelt når respons-en er mottatt."
        }

        val hentFagsystemsbehandlingRespons = hentFagsystemsbehandlingService.lesRespons(respons)
        val feilMelding = hentFagsystemsbehandlingRespons.feilMelding
        if (feilMelding != null) {
            throw UkjentravgrunnlagFeil(
                "Noen gikk galt mens henter fagsystemsbehandling fra fagsystem. " +
                    "Feiler med $feilMelding",
            )
        }
        håndterGamleKravgrunnlagService.håndter(hentFagsystemsbehandlingRespons.hentFagsystemsbehandling!!, mottattXml, task)
    }

    companion object {

        const val TYPE = "gammelKravgrunnlag.håndter"
    }
}
