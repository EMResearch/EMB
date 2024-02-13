package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.person.PersonService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ValiderBrevmottakerService(
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
) {
    fun validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(behandlingId: UUID, fagsakId: UUID) {
        val manuelleBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId).takeIf { it.isNotEmpty() } ?: return
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val bruker = fagsak.bruker
        val fagsystem = fagsak.fagsystem
        val personIdenter = listOfNotNull(bruker.ident)
        if (personIdenter.isEmpty()) return
        val strengtFortroligePersonIdenter = personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter, fagsystem)
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Behandlingen (id: $behandlingId) inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${manuelleBrevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw Feil(melding, frontendFeilmelding)
        }
    }

    fun validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(behandlingId: UUID, fagsakId: UUID) {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val bruker = fagsak.bruker
        val fagsystem = fagsak.fagsystem
        val personIdenter = listOfNotNull(bruker.ident)
        if (personIdenter.isEmpty()) return
        val strengtFortroligePersonIdenter = personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter, fagsystem)
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Behandlingen (id: $behandlingId) inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            val frontendFeilmelding =
                "Behandlingen inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw Feil(melding, frontendFeilmelding)
        }
    }
}
