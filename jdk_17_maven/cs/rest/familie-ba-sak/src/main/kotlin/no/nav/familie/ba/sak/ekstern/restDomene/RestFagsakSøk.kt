package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class RestSøkParam(
    val personIdent: String,
    val barnasIdenter: List<String> = emptyList(),
)

enum class FagsakDeltagerRolle {
    BARN,
    FORELDER,
    UKJENT,
}

data class RestFagsakDeltager(
    val navn: String? = null,
    val ident: String = "",
    val rolle: FagsakDeltagerRolle,
    val fagsakType: FagsakType? = null,
    val kjønn: Kjønn? = Kjønn.UKJENT,
    val fagsakId: Long? = null,
    val fagsakStatus: FagsakStatus? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
) {

    override fun toString(): String {
        return "RestFagsakDeltager(rolle=$rolle, kjønn=$kjønn, fagsakId=$fagsakId)"
    }
}
