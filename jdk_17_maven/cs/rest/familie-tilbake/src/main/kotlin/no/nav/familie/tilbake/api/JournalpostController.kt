package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.JournalføringService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(private val journalføringService: JournalføringService) {

    @Operation(summary = "Hent dokument fra journalføring")
    @GetMapping("/{behandlingId}/journalpost/{journalpostId}/dokument/{dokumentInfoId}")
    @Rolletilgangssjekk(Behandlerrolle.VEILEDER, "Henter journalført dokument", AuditLoggerEvent.ACCESS, HenteParam.BEHANDLING_ID)
    fun hentDokument(
        @PathVariable behandlingId: UUID,
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): Ressurs<ByteArray> {
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId), "OK")
    }

    @Operation(summary = "Hent journalpost informasjon")
    @GetMapping("/{behandlingId}/journalposter")
    @Rolletilgangssjekk(Behandlerrolle.VEILEDER, "Henter journalført dokument", AuditLoggerEvent.ACCESS, HenteParam.BEHANDLING_ID)
    fun hentJournalposter(@PathVariable behandlingId: UUID): Ressurs<List<Journalpost>> {
        return Ressurs.success(journalføringService.hentJournalposter(behandlingId))
    }
}
