package no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class EndringstidspunktController(
    val vedtaksperiodeService: VedtaksperiodeService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    @GetMapping("/behandlinger/{behandlingId}/endringstidspunkt")
    fun hentEndringstidspunkt(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<LocalDate>> = ResponseEntity.ok(
        Ressurs.success(
            behandlingHentOgPersisterService.hent(behandlingId).overstyrtEndringstidspunkt
                ?: vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId),
        ),
    )
}
