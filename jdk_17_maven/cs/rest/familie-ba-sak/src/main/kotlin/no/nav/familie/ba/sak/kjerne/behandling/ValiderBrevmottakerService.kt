package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Brevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service

@Service
class ValiderBrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
) {
    fun validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(behandlingId: Long, nyBrevmottaker: Brevmottaker? = null) {
        var brevmottakere = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)
        nyBrevmottaker?.let {
            brevmottakere += it
        }
        brevmottakere.takeIf { it.isNotEmpty() } ?: return
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandlingId) ?: return
        val personIdenter = personopplysningGrunnlag.søkerOgBarn
            .takeIf { it.isNotEmpty() }
            ?.map { it.aktør.aktivFødselsnummer() }
            ?: return
        val strengtFortroligePersonIdenter =
            familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter)
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding = "Behandlingen (id: $behandlingId) inneholder ${strengtFortroligePersonIdenter.size} person(er) med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${brevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }
}
