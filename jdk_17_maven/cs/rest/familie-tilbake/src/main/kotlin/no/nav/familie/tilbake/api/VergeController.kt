package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.VergeService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling/v1/{behandlingId}/verge", produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VergeController(private val vergeService: VergeService) {

    @Operation(summary = "Opprett verge steg på behandling")
    @PostMapping
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Oppretter verge steg på behandling",
        AuditLoggerEvent.CREATE,
        HenteParam.BEHANDLING_ID,
    )
    fun opprettVergeSteg(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        vergeService.opprettVergeSteg(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Fjern verge")
    @PutMapping
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Deaktiverer ev. eksisterende verge.",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun fjernVerge(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        vergeService.fjernVerge(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent verge")
    @GetMapping
    @Rolletilgangssjekk(
        Behandlerrolle.VEILEDER,
        "Henter verge informasjon",
        AuditLoggerEvent.ACCESS,
        HenteParam.BEHANDLING_ID,
    )
    fun hentVerge(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<VergeDto?> {
        return Ressurs.success(vergeService.hentVerge(behandlingId))
    }
}
