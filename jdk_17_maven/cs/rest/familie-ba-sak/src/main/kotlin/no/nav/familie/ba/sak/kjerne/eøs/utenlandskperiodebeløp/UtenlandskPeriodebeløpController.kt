package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import jakarta.validation.Valid
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/differanseberegning/utenlandskperidebeløp")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UtenlandskPeriodebeløpController(
    private val tilgangService: TilgangService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterUtenlandskPeriodebeløp(
        @PathVariable behandlingId: Long,
        @Valid @RequestBody
        restUtenlandskPeriodebeløp: RestUtenlandskPeriodebeløp,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdaterer utenlandsk periodebeløp",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val barnAktører = restUtenlandskPeriodebeløp.barnIdenter.map { personidentService.hentAktør(it) }

        val eksisterendeUtenlandskPeriodeBeløp = utenlandskPeriodebeløpRepository.getById(restUtenlandskPeriodebeløp.id)

        val utenlandskPeriodebeløp =
            restUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører, eksisterendeUtenlandskPeriodeBeløp)

        utenlandskPeriodebeløpService
            .oppdaterUtenlandskPeriodebeløp(BehandlingId(behandlingId), utenlandskPeriodebeløp)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{utenlandskPeriodebeløpId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettUtenlandskPeriodebeløp(
        @PathVariable behandlingId: Long,
        @PathVariable utenlandskPeriodebeløpId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sletter utenlandsk periodebeløp",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        utenlandskPeriodebeløpService.slettUtenlandskPeriodebeløp(BehandlingId(behandlingId), utenlandskPeriodebeløpId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
