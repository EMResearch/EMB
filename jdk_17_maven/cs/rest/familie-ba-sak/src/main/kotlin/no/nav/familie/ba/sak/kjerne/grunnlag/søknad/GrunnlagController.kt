package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.common.BehandlingValidering.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class GrunnlagController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilbakestillService: TilbakestillBehandlingService,
    private val tilgangService: TilgangService,
) {

    @PostMapping(path = ["/{behandlingId}/legg-til-barn"])
    fun leggTilBarnIPersonopplysningsgrunnlag(
        @PathVariable behandlingId: Long,
        @RequestBody
        leggTilBarnDto: LeggTilBarnDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legge til barn",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        validerBehandlingKanRedigeres(behandling)

        persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(
            behandling = behandling,
            nyttBarnIdent = leggTilBarnDto.barnIdent,
        )
        tilbakestillService.initierOgSettBehandlingTilVilkårsvurdering(behandling)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id),
            ),
        )
    }

    class LeggTilBarnDto(val barnIdent: String)
}
