package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tilbakekreving")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TilbakekrevingController(
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService,
) {

    @PostMapping("/{behandlingId}/forhandsvis-varselbrev")
    fun hentForhåndsvisningVarselbrev(
        @PathVariable
        behandlingId: Long,
        @RequestBody
        forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest,
    ): ResponseEntity<Ressurs<ByteArray>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent forhåndsvisning av varselbrev for tilbakekreving",
        )

        return ResponseEntity.ok(
            Ressurs.success(
                tilbakekrevingService.hentForhåndsvisningVarselbrev(
                    behandlingId,
                    forhåndsvisTilbakekrevingsvarselbrevRequest,
                ),
            ),
        )
    }
}

data class ForhåndsvisTilbakekrevingsvarselbrevRequest(
    val fritekst: String,
)
