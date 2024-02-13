package no.nav.familie.ba.sak.ekstern

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Kalles av familie-klage
@RestController
@RequestMapping(
    path = ["/api/klage/"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
@ProtectedWithClaims(issuer = "azuread")
class EksternKlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
) {

    @GetMapping("fagsaker/{fagsakId}/kan-opprette-revurdering-klage")
    fun kanOppretteRevurderingKlage(@PathVariable fagsakId: Long): Ressurs<KanOppretteRevurderingResponse> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Valider vi kan opprette revurdering med årsak klage på fagsak=$fagsakId",
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        )

        if (!SikkerhetContext.kallKommerFraKlage()) {
            throw Feil("Kallet utføres ikke av en autorisert klient")
        }

        return Ressurs.success(klageService.kanOppretteRevurdering(fagsakId))
    }

    @PostMapping("fagsaker/{fagsakId}/opprett-revurdering-klage/")
    fun opprettRevurderingKlage(@PathVariable fagsakId: Long): Ressurs<OpprettRevurderingResponse> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Opprett revurdering med årask klage på fagsak=$fagsakId",
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        )

        if (!SikkerhetContext.kallKommerFraKlage()) {
            throw Feil("Kallet utføres ikke av en autorisert klient")
        }
        return Ressurs.success(
            klageService.validerOgOpprettRevurderingKlage(
                fagsakId,
            ),
        )
    }

    @GetMapping("fagsaker/{fagsakId}/vedtak")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentVedtak(@PathVariable fagsakId: Long): Ressurs<List<FagsystemVedtak>> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilHandlingOgFagsak(
                fagsakId = fagsakId,
                handling = "Kan hente vedtak på fagsak=$fagsakId",
                event = AuditLoggerEvent.ACCESS,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            )
        }

        return Ressurs.success(klageService.hentFagsystemVedtak(fagsakId))
    }
}
