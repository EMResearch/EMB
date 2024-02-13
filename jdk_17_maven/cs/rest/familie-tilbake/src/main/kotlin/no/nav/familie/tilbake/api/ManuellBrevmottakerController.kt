package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerResponsDto
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerMapper
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/brevmottaker/manuell")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ManuellBrevmottakerController(private val manuellBrevmottakerService: ManuellBrevmottakerService) {

    @Operation(summary = "Legger til brevmottaker manuelt")
    @PostMapping(
        path = ["/{behandlingId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Legger til brevmottaker manuelt",
        AuditLoggerEvent.CREATE,
        HenteParam.BEHANDLING_ID,
    )
    fun leggTilBrevmottaker(
        @PathVariable behandlingId: UUID,
        @Valid @RequestBody
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ): Ressurs<UUID> {
        val id = manuellBrevmottakerService.leggTilBrevmottaker(behandlingId, manuellBrevmottakerRequestDto)
        return Ressurs.success(id, melding = "Manuell brevmottaker er lagt til.")
    }

    @Operation(summary = "Henter manuell brevmottakere")
    @GetMapping(
        path = ["/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        handling = "Henter manuelle brevmottakere",
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.BEHANDLING_ID,
    )
    fun hentManuellBrevmottakere(@PathVariable behandlingId: UUID): Ressurs<List<ManuellBrevmottakerResponsDto>> {
        return Ressurs
            .success(
                manuellBrevmottakerService.hentBrevmottakere(behandlingId)
                    .map { ManuellBrevmottakerMapper.tilRespons(it) },
            )
    }

    @Operation(summary = "Oppdaterer manuell brevmottaker")
    @PutMapping(
        path = ["/{behandlingId}/{manuellBrevmottakerId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Oppdaterer manuell brevmottaker",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun oppdaterManuellBrevmottaker(
        @PathVariable behandlingId: UUID,
        @PathVariable manuellBrevmottakerId: UUID,
        @Valid @RequestBody
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ): Ressurs<String> {
        manuellBrevmottakerService.oppdaterBrevmottaker(behandlingId, manuellBrevmottakerId, manuellBrevmottakerRequestDto)
        return Ressurs.success("", melding = "Manuell brevmottaker er oppdatert")
    }

    @Operation(summary = "Fjerner manuell brevmottaker")
    @DeleteMapping(path = ["/{behandlingId}/{manuellBrevmottakerId}"])
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        handling = "Fjerner manuell brevmottaker",
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
    )
    fun fjernManuellBrevmottaker(
        @PathVariable behandlingId: UUID,
        @PathVariable manuellBrevmottakerId: UUID,
    ): Ressurs<String> {
        manuellBrevmottakerService.fjernBrevmottaker(behandlingId, manuellBrevmottakerId)
        return Ressurs.success("", melding = "Manuell brevmottaker er fjernet")
    }

    @Operation(summary = "Opprett og aktiver brevmottaker-steg på behandling")
    @PostMapping(path = ["/{behandlingId}/aktiver"])
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Oppretter brevmottaker-steg på behandling",
        AuditLoggerEvent.CREATE,
        HenteParam.BEHANDLING_ID,
    )
    fun opprettBrevmottakerSteg(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        manuellBrevmottakerService.opprettBrevmottakerSteg(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Fjern manuelle brevmottakere og deaktiver steg")
    @PutMapping(path = ["/{behandlingId}/deaktiver"])
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Fjern ev. manuelt registrerte brevmottakere og deaktiver steg.",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun fjernBrevmottakerSteg(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        manuellBrevmottakerService.fjernManuelleBrevmottakereOgTilbakeførSteg(behandlingId)
        return Ressurs.success("OK")
    }
}
