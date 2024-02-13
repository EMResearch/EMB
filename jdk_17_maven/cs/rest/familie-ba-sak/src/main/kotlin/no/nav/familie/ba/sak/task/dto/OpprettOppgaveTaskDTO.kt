package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate

data class OpprettOppgaveTaskDTO(
    val behandlingId: Long,
    val oppgavetype: Oppgavetype,
    val fristForFerdigstillelse: LocalDate,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String?,
    val manuellOppgaveType: ManuellOppgaveType? = null,
)

enum class ManuellOppgaveType(val settBehandlesAvApplikasjon: Boolean) {
    SMÅBARNSTILLEGG(true),
    FØDSELSHENDELSE(false),
    ÅPEN_BEHANDLING(true),
}
