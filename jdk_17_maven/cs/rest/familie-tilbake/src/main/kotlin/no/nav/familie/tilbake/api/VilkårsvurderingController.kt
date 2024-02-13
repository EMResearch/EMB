package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.VurdertVilkårsvurderingDto
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårsvurderingController(val vilkårsvurderingService: VilkårsvurderingService) {

    @Operation(summary = "Hent vilkårsvurdering")
    @GetMapping(
        path = ["{behandlingId}/vilkarsvurdering/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.VEILEDER,
        "Henter vilkårsvurdering for en gitt behandling",
        AuditLoggerEvent.ACCESS,
        HenteParam.BEHANDLING_ID,
    )
    fun hentVurdertVilkårsvurdering(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<VurdertVilkårsvurderingDto> {
        return Ressurs.success(vilkårsvurderingService.hentVilkårsvurdering(behandlingId))
    }
}
