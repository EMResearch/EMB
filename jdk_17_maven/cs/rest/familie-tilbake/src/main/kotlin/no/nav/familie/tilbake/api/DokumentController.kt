package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.api.dto.BestillBrevDto
import no.nav.familie.tilbake.api.dto.ForhåndsvisningHenleggelsesbrevDto
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.behandling.LagreUtkastVedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.DokumentbehandlingService
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Avsnitt
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentController(
    private val varselbrevService: VarselbrevService,
    private val dokumentbehandlingService: DokumentbehandlingService,
    private val henleggelsesbrevService: HenleggelsesbrevService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val lagreUtkastVedtaksbrevService: LagreUtkastVedtaksbrevService,
) {

    @Operation(summary = "Bestill brevsending")
    @PostMapping("/bestill")
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Sender brev", AuditLoggerEvent.CREATE)
    fun bestillBrev(
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<Nothing?> {
        val maltype: Dokumentmalstype = bestillBrevDto.brevmalkode
        dokumentbehandlingService.bestillBrev(bestillBrevDto.behandlingId, maltype, bestillBrevDto.fritekst)
        return Ressurs.success(null)
    }

    @Operation(summary = "Forhåndsvis brev")
    @PostMapping("/forhandsvis")
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Forhåndsviser brev", AuditLoggerEvent.ACCESS)
    fun forhåndsvisBrev(
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<ByteArray> {
        val dokument: ByteArray = dokumentbehandlingService.forhåndsvisBrev(
            bestillBrevDto.behandlingId,
            bestillBrevDto.brevmalkode,
            bestillBrevDto.fritekst,
        )
        return Ressurs.success(dokument)
    }

    @Operation(summary = "Forhåndsvis varselbrev")
    @PostMapping(
        "/forhandsvis-varselbrev",
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Forhåndsviser brev", AuditLoggerEvent.ACCESS)
    fun hentForhåndsvisningVarselbrev(
        @Valid @RequestBody
        forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest,
    ): ByteArray {
        return varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
    }

    @Operation(summary = "Forhåndsvis henleggelsesbrev")
    @PostMapping(
        "/forhandsvis-henleggelsesbrev",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Forhåndsviser henleggelsesbrev", AuditLoggerEvent.ACCESS)
    fun hentForhåndsvisningHenleggelsesbrev(
        @Valid @RequestBody
        dto: ForhåndsvisningHenleggelsesbrevDto,
    ): Ressurs<ByteArray> {
        return Ressurs.success(henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(dto.behandlingId, dto.fritekst))
    }

    @Operation(summary = "Forhåndsvis vedtaksbrev")
    @PostMapping(
        "/forhandsvis-vedtaksbrev",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Forhåndsviser brev", AuditLoggerEvent.ACCESS)
    fun hentForhåndsvisningVedtaksbrev(
        @Valid @RequestBody
        dto: HentForhåndvisningVedtaksbrevPdfDto,
    ): Ressurs<ByteArray> {
        return Ressurs.success(vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto))
    }

    @Operation(summary = "Hent vedtaksbrevtekst")
    @GetMapping(
        "/vedtaksbrevtekst/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.VEILEDER, "Henter vedtaksbrevtekst", AuditLoggerEvent.ACCESS, HenteParam.BEHANDLING_ID)
    fun hentVedtaksbrevtekst(@PathVariable behandlingId: UUID): Ressurs<List<Avsnitt>> {
        return Ressurs.success(vedtaksbrevService.hentVedtaksbrevSomTekst(behandlingId))
    }

    @Operation(summary = "Lagre utkast av vedtaksbrev")
    @PostMapping(
        "/vedtaksbrevtekst/{behandlingId}/utkast",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Lagrer utkast av vedtaksbrev",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun lagreUtkastVedtaksbrev(
        @PathVariable behandlingId: UUID,
        @RequestBody fritekstavsnitt: FritekstavsnittDto,
    ): Ressurs<String> {
        lagreUtkastVedtaksbrevService.lagreUtkast(behandlingId, fritekstavsnitt)
        return Ressurs.success("OK")
    }
}
