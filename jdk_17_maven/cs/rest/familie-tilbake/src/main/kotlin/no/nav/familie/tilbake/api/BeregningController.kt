package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.BeregnetPerioderDto
import no.nav.familie.tilbake.api.dto.BeregningsresultatDto
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(val tilbakekrevingsberegningService: TilbakekrevingsberegningService) {

    @Operation(summary = "Beregn feilutbetalt beløp for nye delte perioder")
    @PostMapping(
        path = ["{behandlingId}/beregn/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        handling = "Beregner feilutbetalt beløp for nye delte perioder",
        AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.BEHANDLING_ID,
    )
    fun beregnBeløp(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        perioder: List<Datoperiode>,
    ): Ressurs<BeregnetPerioderDto> {
        return Ressurs.success(tilbakekrevingsberegningService.beregnBeløp(behandlingId, perioder))
    }

    @Operation(summary = "Hent beregningsresultat")
    @GetMapping(
        path = ["{behandlingId}/beregn/resultat/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        handling = "Henter beregningsresultat",
        AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.BEHANDLING_ID,
    )
    fun hentBeregningsresultat(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<BeregningsresultatDto> {
        return Ressurs.success(tilbakekrevingsberegningService.hentBeregningsresultat(behandlingId))
    }
}
