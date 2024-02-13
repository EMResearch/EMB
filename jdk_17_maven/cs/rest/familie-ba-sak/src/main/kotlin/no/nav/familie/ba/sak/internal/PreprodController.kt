package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/preprod")
@ProtectedWithClaims(issuer = "azuread")
class PreprodController(
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val environment: Environment,
) {

    @PutMapping(path = ["/{behandlingId}/fyll-ut-vilkarsvurdering"])
    fun settFomPåTommeVilkårTilFødselsdato(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<String>> {
        val erProd = environment.activeProfiles.any { it == Profil.Prod.navn.trim() }
        val erDevPostgresPreprod = environment.activeProfiles.any { it == Profil.DevPostgresPreprod.navn.trim() }
        val erPreprod = environment.activeProfiles.any { it == Profil.Preprod.navn.trim() }

        if (erProd) {
            throw Feil("Skal ikke være tilgjengelig i prod")
        } else if (!(erDevPostgresPreprod || erPreprod)) {
            throw Feil("Skal bare være tilgjengelig i for preprod eller lokalt")
        }

        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.CREATE)

        testVerktøyService.oppdaterVilkårUtenFomTilFødselsdato(behandlingId)

        return ResponseEntity.ok(Ressurs.success("Oppdaterte vilkårsvurdering"))
    }
}
