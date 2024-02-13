package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.ByttEnhetDto
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.api.dto.OpprettRevurderingDto
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val stegService: StegService,
) {

    @Operation(summary = "Opprett tilbakekrevingsbehandling automatisk, kan kalles av fagsystem, batch")
    @PostMapping(
        path = ["/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Oppretter tilbakekreving", AuditLoggerEvent.CREATE)
    fun opprettBehandling(
        @Valid @RequestBody
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ): Ressurs<String> {
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Behandling er opprettet.")
    }

    @Operation(summary = "Opprett tilbakekrevingsbehandling manuelt")
    @PostMapping(
        path = ["/manuelt/task/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Oppretter tilbakekreving manuelt", AuditLoggerEvent.CREATE)
    fun opprettBehandlingManuellTask(
        @Valid @RequestBody
        opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest,
    ): Ressurs<String> {
        behandlingService.opprettBehandlingManuellTask(opprettManueltTilbakekrevingRequest)
        return Ressurs.success("Manuell opprettelse av tilbakekreving er startet")
    }

    @Operation(summary = "Opprett tilbakekrevingsrevurdering")
    @PostMapping(
        path = ["/revurdering/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(Behandlerrolle.SAKSBEHANDLER, "Oppretter tilbakekrevingsrevurdering", AuditLoggerEvent.CREATE)
    fun opprettRevurdering(
        @Valid @RequestBody
        opprettRevurderingDto: OpprettRevurderingDto,
    ): Ressurs<String> {
        val behandling = behandlingService.opprettRevurdering(opprettRevurderingDto)
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Revurdering er opprettet.")
    }

    @Operation(summary = "Hent behandling")
    @GetMapping(
        path = ["/v1/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        handling = "Henter tilbakekrevingsbehandling",
        AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.BEHANDLING_ID,
    )
    fun hentBehandling(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<BehandlingDto> {
        return Ressurs.success(behandlingService.hentBehandling(behandlingId))
    }

    @Operation(summary = "Utfør behandlingssteg og fortsett behandling til neste steg")
    @PostMapping(
        path = ["{behandlingId}/steg/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    // Rollen blir endret til BESLUTTER i Tilgangskontroll for FatteVedtak steg
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Utfører behandlingens aktiv steg og fortsetter den til neste steg",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun utførBehandlingssteg(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        behandlingsstegDto: BehandlingsstegDto,
    ): Ressurs<String> {
        // Oppdaterer ansvarlig saksbehandler først slik at historikkinnslag får riktig saksbehandler
        // Hvis det feiler noe,bør det rullet tilbake helt siden begge 2 er på samme transaksjon
        if (stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)) {
            behandlingService.oppdaterAnsvarligSaksbehandler(behandlingId)
        }
        stegService.håndterSteg(behandlingId, behandlingsstegDto)

        return Ressurs.success("OK")
    }

    @Operation(summary = "Sett behandling på vent")
    @PutMapping(
        path = ["{behandlingId}/vent/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Setter saksbehandler behandling på vent eller utvider fristen",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun settBehandlingPåVent(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        behandlingPåVentDto: BehandlingPåVentDto,
    ): Ressurs<String> {
        behandlingService.settBehandlingPåVent(behandlingId, behandlingPåVentDto)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Ta behandling av vent")
    @PutMapping(
        path = ["{behandlingId}/gjenoppta/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Saksbehandler tar behandling av vent etter å motta brukerrespons eller dokumentasjon",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun taBehandlingAvVent(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        behandlingService.taBehandlingAvvent(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Henlegg behandling")
    @PutMapping(
        path = ["{behandlingId}/henlegg/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Saksbehandler henlegger behandling",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun henleggBehandling(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        henleggelsesbrevFritekstDto: HenleggelsesbrevFritekstDto,
    ): Ressurs<String> {
        behandlingService.henleggBehandling(behandlingId, henleggelsesbrevFritekstDto)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Bytt enhet")
    @PutMapping(
        path = ["{behandlingId}/bytt-enhet/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Saksbehandler bytter enhet på behandling",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun byttEnhet(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        byttEnhetDto: ByttEnhetDto,
    ): Ressurs<String> {
        behandlingService.byttBehandlendeEnhet(behandlingId, byttEnhetDto)
        return Ressurs.success("OK")
    }
}
