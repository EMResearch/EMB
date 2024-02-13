package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/fagsaker"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
) {

    @PostMapping("/{fagsakId}/opprett-klagebehandling")
    fun opprettKlage(@PathVariable fagsakId: Long, @RequestBody opprettKlageDto: OpprettKlageDto): Ressurs<Long> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett klagebehandling",
        )
        klageService.opprettKlage(fagsakId, opprettKlageDto)
        return Ressurs.success(fagsakId)
    }

    @GetMapping("/{fagsakId}/hent-klagebehandlinger")
    fun hentKlagebehandlinger(@PathVariable fagsakId: Long): Ressurs<List<KlagebehandlingDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent klagebehandlinger på fagsak",
        )
        return Ressurs.success(klageService.hentKlagebehandlingerPåFagsak(fagsakId))
    }
}
