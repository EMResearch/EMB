package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestFeilutbetaltValuta
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feilutbetalt-valuta")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FeilutbetaltValutaController(
    private val tilgangService: TilgangService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @PostMapping(
        path = ["behandling/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun leggTilFeilutbetaltValutaPeriode(
        @PathVariable behandlingId: Long,
        @RequestBody feilutbetaltValuta: RestFeilutbetaltValuta,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legg til periode med feilutbetalt valuta",
        )

        feilutbetaltValutaService.leggTilFeilutbetaltValutaPeriode(
            feilutbetaltValuta = feilutbetaltValuta,
            behandlingId = behandlingId,
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(
        path = ["behandling/{behandlingId}/periode/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterFeilutbetaltValutaPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long,
        @RequestBody feilutbetaltValuta: RestFeilutbetaltValuta,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdater periode med feilutbetalt valuta",
        )

        feilutbetaltValutaService.oppdatertFeilutbetaltValutaPeriode(feilutbetaltValuta = feilutbetaltValuta, id = id)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["behandling/{behandlingId}/periode/{id}"])
    fun fjernFeilutbetaltValutaPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Fjerner periode med feilutbetalt valuta",
        )
        feilutbetaltValutaService.fjernFeilutbetaltValutaPeriode(id = id, behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFeilutbetaltValutaPerioder(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<List<RestFeilutbetaltValuta>?>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente feilutbetalt valuta for behandling",
        )
        return ResponseEntity.ok(Ressurs.success(feilutbetaltValutaService.hentFeilutbetaltValutaPerioder(behandlingId = behandlingId)))
    }
}
