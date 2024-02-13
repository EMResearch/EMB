package no.nav.familie.tilbake.behandling.task

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterFaktainfoTask.TYPE,
    beskrivelse = "oppdaterer fakta info når kravgrunnlag mottas av ny referanse",
    maxAntallFeil = 10,
    triggerTidVedFeilISekunder = 5L,
)
class OppdaterFaktainfoTask(
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterFaktainfoTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val eksternFagsakId = task.metadata.getProperty("eksternFagsakId")
        val ytelsestype = Ytelsestype.valueOf(task.metadata.getProperty("ytelsestype"))
        val eksternId = task.metadata.getProperty("eksternId")

        val requestSendt = requireNotNull(
            hentFagsystemsbehandlingService.hentFagsystemsbehandlingRequestSendt(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            ),
        )
        // kaster exception inntil respons-en har mottatt
        val hentFagsystemsbehandlingRespons = requireNotNull(requestSendt.respons) {
            "HentFagsystemsbehandlingRespons er ikke mottatt fra fagsystem for " +
                "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype,eksternId=$eksternId." +
                "Task kan kjøre på nytt manuelt når respons er mottatt."
        }

        val respons = hentFagsystemsbehandlingService.lesRespons(hentFagsystemsbehandlingRespons)
        val feilMelding = respons.feilMelding
        if (feilMelding != null) {
            throw Feil(
                "Noen gikk galt mens henter fagsystemsbehandling fra fagsystem. " +
                    "Feiler med $feilMelding",
            )
        }
        behandlingService.oppdaterFaktainfo(eksternFagsakId, ytelsestype, eksternId, respons.hentFagsystemsbehandling!!)
    }

    companion object {

        const val TYPE = "oppdater.faktainfo"
    }
}
