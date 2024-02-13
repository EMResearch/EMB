package no.nav.familie.ba.sak.integrasjoner.oppgave.domene

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate
import java.time.LocalDateTime

data class RestFinnOppgaveRequest(
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String? = null,
    val enhet: String? = null,
    val saksbehandler: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val tildeltRessurs: Boolean? = null,
    val opprettetFomTidspunkt: LocalDateTime? = null,
    val opprettetTomTidspunkt: LocalDateTime? = null,
    val fristFomDato: LocalDate? = null,
    val fristTomDato: LocalDate? = null,
    val aktivFomDato: LocalDate? = null,
    val aktivTomDato: LocalDate? = null,
    val limit: Long? = null,
    val offset: Long? = null,
) {

    fun tilFinnOppgaveRequest(): FinnOppgaveRequest = FinnOppgaveRequest(
        tema = Tema.BAR,
        behandlingstema = Behandlingstema.values().find { it.value == this.behandlingstema },
        behandlingstype = Behandlingstype.values().find { it.value == this.behandlingstype },
        oppgavetype = Oppgavetype.values().find { it.value == this.oppgavetype },
        enhet = this.enhet,
        saksbehandler = this.saksbehandler,
        journalpostId = this.journalpostId,
        tildeltRessurs = this.tildeltRessurs,
        tilordnetRessurs = this.tilordnetRessurs,
        opprettetFomTidspunkt = this.opprettetFomTidspunkt,
        opprettetTomTidspunkt = this.opprettetTomTidspunkt,
        fristFomDato = this.fristFomDato,
        fristTomDato = this.fristTomDato,
        aktivFomDato = this.aktivFomDato,
        aktivTomDato = this.aktivTomDato,
        limit = this.limit,
        offset = this.offset,
    )
}
