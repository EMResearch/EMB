package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.AktørId
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

data class OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO(
    val ident: AktørId,
    val oppgavetype: Oppgavetype,
    val beskrivelse: String,
)
