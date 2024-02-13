package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.config.PropertyName
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lager oppgave for nye behandlinger",
    triggerTidVedFeilISekunder = 300L,
)
class LagOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val oppgavePrioritetService: OppgavePrioritetService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        val saksbehandler = task.metadata.getProperty("saksbehandler")
        val enhet = task.metadata.getProperty(PropertyName.ENHET) ?: "" // elvis-operator for bakoverkompatibilitet
        val behandlingId = UUID.fromString(task.payload)

        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)

        val sendtTilBeslutningAv: String? = if (behandlingsstegstilstand?.behandlingssteg == Behandlingssteg.FATTE_VEDTAK) {
            task.metadata.getProperty("opprettetAv")?.let { "Sendt til godkjenning av $it" }
        } else {
            null
        }

        val fristeUker = behandlingsstegstilstand?.venteårsak?.defaultVenteTidIUker ?: 0
        val venteårsak = behandlingsstegstilstand?.venteårsak?.beskrivelse ?: ""
        val beskrivelse = sendtTilBeslutningAv?.let { "$sendtTilBeslutningAv $venteårsak" } ?: venteårsak
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId)

        oppgaveService.opprettOppgave(
            UUID.fromString(task.payload),
            oppgavetype,
            enhet,
            beskrivelse,
            LocalDate.now().plusWeeks(fristeUker),
            saksbehandler,
            prioritet,
        )
    }

    companion object {

        const val TYPE = "lagOppgave"
    }
}
