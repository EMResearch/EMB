package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
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
@RequestMapping("/api/test/kompetanser")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class KompetanseTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val kompetanseService: KompetanseService,
) {

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreKompetanser(
        @PathVariable behandlingId: Long,
        @RequestBody restKompetanser: Map<LocalDate, String>,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandlingIdObjekt = BehandlingId(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingIdObjekt.id)!!
        restKompetanser.tilKompetanser(behandlingIdObjekt, personopplysningGrunnlag).forEach {
            kompetanseService.oppdaterKompetanse(behandlingIdObjekt, it)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingIdObjekt.id)))
    }
}

private fun Map<LocalDate, String>.tilKompetanser(
    behandlingId: BehandlingId,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Collection<Kompetanse> {
    return this.map { (dato, tidslinje) ->
        val person = personopplysningGrunnlag.personer.first { it.fødselsdato == dato }
        KompetanseBuilder(dato.tilMånedTidspunkt(), behandlingId)
            .medKompetanse(tidslinje, person)
            .byggKompetanser()
    }.flatten().slåSammen()
}
