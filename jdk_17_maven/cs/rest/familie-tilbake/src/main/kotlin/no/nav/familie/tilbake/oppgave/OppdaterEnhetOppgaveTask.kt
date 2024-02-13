package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.config.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterEnhetOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer enhet på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterEnhetOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterEnhetOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val enhetId = task.metadata.getProperty("enhetId")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val saksbehandler = task.metadata.getProperty("saksbehandler")
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val nyBeskrivelse = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm")) + ":" +
            beskrivelse + System.lineSeparator() + oppgave.beskrivelse
        var patchetOppgave = oppgave.copy(beskrivelse = nyBeskrivelse)
        if (!saksbehandler.isNullOrEmpty() && saksbehandler != Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            patchetOppgave = patchetOppgave.copy(tilordnetRessurs = saksbehandler)
        }
        oppgaveService.patchOppgave(patchetOppgave)

        if (oppgave.tema == Tema.ENF) {
            oppgaveService.tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, false) // ENF bruker generelle mapper
        } else {
            oppgaveService.tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, true) // KON og BAR bruker mapper som hører til enhetene
        }
    }

    companion object {

        const val TYPE = "oppdaterEnhetOppgave"
    }
}
