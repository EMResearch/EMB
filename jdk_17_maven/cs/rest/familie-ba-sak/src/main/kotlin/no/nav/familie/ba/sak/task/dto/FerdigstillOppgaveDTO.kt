package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

data class FerdigstillOppgaveDTO(
    val behandlingId: Long,
    val oppgavetype: Oppgavetype,
)
