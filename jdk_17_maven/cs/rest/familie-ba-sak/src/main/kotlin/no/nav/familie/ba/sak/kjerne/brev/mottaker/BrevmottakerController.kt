package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brevmottaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevmottakerController(
    private val tilgangService: TilgangService,
    private val brevmottakerService: BrevmottakerService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @PostMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    fun leggTilBrevmottaker(
        @PathVariable behandlingId: Long,
        @RequestBody brevmottaker: RestBrevmottaker,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legge til brevmottaker",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)
        brevmottakerService.leggTilBrevmottaker(brevmottaker, behandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{mottakerId}"])
    fun fjernBrevmottaker(
        @PathVariable behandlingId: Long,
        @PathVariable mottakerId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "fjerne brevmottaker",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)
        brevmottakerService.fjernBrevmottaker(id = mottakerId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE])
    fun hentBrevmottakere(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<List<RestBrevmottaker>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente brevmottakere",
        )
        return ResponseEntity.ok(Ressurs.success(brevmottakerService.hentRestBrevmottakere(behandlingId = behandlingId)))
    }
}
