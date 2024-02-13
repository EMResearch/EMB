package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.NotNull
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FaktaFeilutbetalingController(val faktaFeilutbetalingService: FaktaFeilutbetalingService) {

    @Operation(summary = "Hent fakta om feilutbetaling")
    @GetMapping(
        path = ["/behandling/{behandlingId}/fakta/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.VEILEDER,
        "Henter fakta om feilutbetaling for en gitt behandling",
        AuditLoggerEvent.ACCESS,
        HenteParam.BEHANDLING_ID,
    )
    fun hentFaktaomfeilutbetaling(
        @NotNull
        @PathVariable("behandlingId")
        behandlingId: UUID,
    ): Ressurs<FaktaFeilutbetalingDto> {
        return Ressurs.success(faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId))
    }
}
