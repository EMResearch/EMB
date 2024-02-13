package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType

data class RestHentFagsakForPerson(val personIdent: String, val fagsakType: FagsakType = FagsakType.NORMAL)
