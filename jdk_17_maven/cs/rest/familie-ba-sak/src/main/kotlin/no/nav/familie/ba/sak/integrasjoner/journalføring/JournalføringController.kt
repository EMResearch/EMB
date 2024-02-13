package no.nav.familie.ba.sak.integrasjoner.journalføring

import jakarta.validation.Valid
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(
    private val innkommendeJournalføringService: InnkommendeJournalføringService,
    private val tilgangService: TilgangService,
) {

    @GetMapping(path = ["/{journalpostId}/hent"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentJournalpost(@PathVariable journalpostId: String): ResponseEntity<Ressurs<Journalpost>> {
        return ResponseEntity.ok(Ressurs.success(innkommendeJournalføringService.hentJournalpost(journalpostId)))
    }

    @PostMapping(path = ["/for-bruker"])
    fun hentJournalposterForBruker(@RequestBody personIdentBody: PersonIdent): ResponseEntity<Ressurs<List<Journalpost>>> {
        return ResponseEntity.ok(
            Ressurs.success(
                innkommendeJournalføringService.hentJournalposterForBruker(
                    personIdentBody.ident,
                ),
            ),
        )
    }

    @GetMapping("/{journalpostId}/hent/{dokumentInfoId}")
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ResponseEntity<Ressurs<ByteArray>> {
        return ResponseEntity.ok(
            Ressurs.success(
                innkommendeJournalføringService.hentDokument(
                    journalpostId,
                    dokumentInfoId,
                ),
            ),
        )
    }

    @GetMapping(
        path = ["/{journalpostId}/dokument/{dokumentInfoId}"],
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun hentDokumentBytearray(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ResponseEntity<ByteArray> {
        return ResponseEntity.ok(innkommendeJournalføringService.hentDokument(journalpostId, dokumentInfoId))
    }

    @PostMapping(path = ["/{journalpostId}/journalfør/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun journalførV2(
        @PathVariable journalpostId: String,
        @PathVariable oppgaveId: String,
        @RequestParam(name = "journalfoerendeEnhet") journalførendeEnhet: String,
        @RequestBody @Valid
        request: RestJournalføring,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "journalføre",
        )

        if (request.dokumenter.any { it.dokumentTittel == null || it.dokumentTittel == "" }) {
            throw FunksjonellFeil("Minst ett av dokumentene mangler dokumenttittel.")
        }

        val fagsakId =
            innkommendeJournalføringService.journalfør(request, journalpostId, journalførendeEnhet, oppgaveId)
        return ResponseEntity.ok(Ressurs.success(fagsakId, "Journalpost $journalpostId Journalført"))
    }
}
