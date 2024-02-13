package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

// Denne kontrollen inneholder tjenester som kun brukes av forvaltningsteam via swagger. Frontend bør ikke kalle disse tjenestene.

@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(private val forvaltningService: ForvaltningService) {

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
        @PathVariable eksternKravgrunnlagId: BigInteger,
    ): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId, eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Arkiver mottatt kravgrunnlag")
    @PutMapping(
        path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Arkiverer mottatt kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.MOTTATT_XML_ID,
    )
    fun arkiverMottattKravgrunnlag(@PathVariable mottattXmlId: UUID): Ressurs<String> {
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Tvinghenlegg behandling")
    @PutMapping(
        path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.FORVALTER, "Tving henlegger behandling", AuditLoggerEvent.NONE, HenteParam.BEHANDLING_ID)
    fun tvingHenleggBehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(
        path = ["/behandling/{behandlingId}/flytt-behandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Flytter behandling tilbake til Fakta",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun flyttBehandlingTilFakta(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Annuler kravgrunnlag")
    @PutMapping(
        path = ["/annuler/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Annulerer kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.EKSTERN_KRAVGRUNNLAG_ID,
    )
    fun annulerKravgrunnlag(@PathVariable eksternKravgrunnlagId: BigInteger): Ressurs<String> {
        forvaltningService.annulerKravgrunnlag(eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent informasjon som kreves for forvaltning")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter forvaltningsinformasjon",
        AuditLoggerEvent.NONE,
        HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentForvaltningsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Forvaltningsinfo>> {
        return Ressurs.success(forvaltningService.hentForvaltningsinfo(ytelsestype, eksternFagsakId))
    }
}

data class Forvaltningsinfo(
    val eksternKravgrunnlagId: BigInteger,
    val kravgrunnlagId: UUID?,
    val kravgrunnlagKravstatuskode: String?,
    val mottattXmlId: UUID?,
    val eksternId: String,
    val opprettetTid: LocalDateTime,
    val behandlingId: UUID?,
)
