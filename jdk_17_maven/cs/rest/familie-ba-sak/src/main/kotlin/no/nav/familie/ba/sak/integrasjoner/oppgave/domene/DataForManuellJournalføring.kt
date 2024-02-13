package no.nav.familie.ba.sak.integrasjoner.oppgave.domene

import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave

data class DataForManuellJournalføring(
    val oppgave: Oppgave,
    val person: RestPersonInfo?,
    val journalpost: Journalpost?,
    val minimalFagsak: RestMinimalFagsak?,
)
