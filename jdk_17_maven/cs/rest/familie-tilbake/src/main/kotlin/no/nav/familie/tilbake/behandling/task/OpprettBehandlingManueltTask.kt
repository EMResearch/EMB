package no.nav.familie.tilbake.behandling.task

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingManuellOpprettelseService
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettBehandlingManueltTask.TYPE,
    beskrivelse = "oppretter behandling manuelt",
    maxAntallFeil = 10,
    triggerTidVedFeilISekunder = 5L,
)
class OpprettBehandlingManueltTask(
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val behManuellOpprService: BehandlingManuellOpprettelseService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun preCondition(task: Task) {
        log.info("Henter fagsystemsbehandling for OpprettBehandlingManueltTask med id ${task.id} og metadata ${task.metadata}")
        val eksternFagsakId = task.metadata.getProperty("eksternFagsakId")
        val ytelsestype = Ytelsestype.valueOf(task.metadata.getProperty("ytelsestype"))
        val eksternId = task.metadata.getProperty("eksternId")
        hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
    }

    @Transactional
    override fun doTask(task: Task) {
        log.info("OpprettBehandlingManueltTask prosesserer med id=${task.id} og metadata ${task.metadata}")
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
        val respons = requireNotNull(requestSendt.respons) {
            "HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
                "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype,eksternId=$eksternId." +
                "Task-en kan kjøre på nytt manuelt når respons-en er mottatt"
        }

        val hentFagsystemsbehandlingRespons = hentFagsystemsbehandlingService.lesRespons(respons)
        val feilMelding = hentFagsystemsbehandlingRespons.feilMelding
        if (feilMelding != null) {
            hentFagsystemsbehandlingService.slettOgSendNyHentFagsystembehandlingRequest(
                requestSendtId = requestSendt.id,
                eksternFagsakId = eksternFagsakId,
                ytelsestype = ytelsestype,
                eksternId = eksternId,
            )
            throw Feil(
                "Noe gikk galt ved henting av fagsystemsbehandling fra fagsystem. Legger ny melding på topic. Task må rekjøres. " +
                    "Feiler med $feilMelding",
            )
        }

        // opprett behandling
        val ansvarligSaksbehandler = task.metadata.getProperty("ansvarligSaksbehandler")
        log.info(
            "Oppretter manuell tilbakekrevingsbehandling request for " +
                "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype,eksternId=$eksternId.",
        )
        behManuellOpprService.opprettBehandlingManuell(
            eksternFagsakId = eksternFagsakId,
            ytelsestype = ytelsestype,
            eksternId = eksternId,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            fagsystemsbehandlingData = hentFagsystemsbehandlingRespons
                .hentFagsystemsbehandling!!,
        )
    }

    companion object {

        const val TYPE = "opprettBehandlingManuelt"
    }
}
