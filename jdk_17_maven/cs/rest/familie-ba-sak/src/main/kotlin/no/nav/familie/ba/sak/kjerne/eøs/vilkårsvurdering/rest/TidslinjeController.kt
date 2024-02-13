package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tidslinjer")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TidslinjeController(
    private val tilgangService: TilgangService,
    private val tidslinjeService: VilkårsvurderingTidslinjeService,
) {

    @GetMapping("/{behandlingId}")
    fun hentTidslinjer(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestTidslinjer>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Henter tidslinjer",
        )
        return ResponseEntity.ok(
            success(
                tidslinjeService.hentTidslinjerThrows(BehandlingId(behandlingId)).tilRestTidslinjer(),
            ),
        )
    }
}
