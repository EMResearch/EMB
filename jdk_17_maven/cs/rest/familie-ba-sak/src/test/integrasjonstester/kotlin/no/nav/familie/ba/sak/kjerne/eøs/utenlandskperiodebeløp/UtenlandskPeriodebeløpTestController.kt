package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/utenlandskeperiodebeløp")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class UtenlandskPeriodebeløpTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
) {

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreUtenlandskePeriodebeløp(
        @PathVariable behandlingId: Long,
        @RequestBody restUtenlandskePeriodebeløp: Map<LocalDate, String>,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandlingIdObjekt = BehandlingId(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingIdObjekt.id)!!
        restUtenlandskePeriodebeløp.tilUtenlandskePeriodebeløp(behandlingIdObjekt, personopplysningGrunnlag).forEach {
            utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(behandlingIdObjekt, it)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingIdObjekt.id)))
    }
}

private fun Map<LocalDate, String>.tilUtenlandskePeriodebeløp(
    behandlingId: BehandlingId,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Collection<UtenlandskPeriodebeløp> {
    return this.map { (dato, tidslinje) ->
        val person = personopplysningGrunnlag.personer.first { it.fødselsdato == dato }
        UtenlandskPeriodebeløpBuilder(dato.tilMånedTidspunkt(), behandlingId)
            .medBeløp(tidslinje, "EUR", "fr", person)
            .bygg()
    }.flatten().slåSammen()
}
