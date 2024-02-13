package no.nav.familie.ba.sak.statistikk.internstatistikk

import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.SøknadsstatistikkForPeriode
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/internstatistikk")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InternStatistikkController(
    private val internStatistikkService: InternStatistikkService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAntallFagsakerOpprettet(): ResponseEntity<Ressurs<InternStatistikkResponse>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter internstatistikk")
        val internstatistikk = InternStatistikkResponse(
            antallFagsakerTotalt = internStatistikkService.finnAntallFagsakerTotalt(),
            antallFagsakerLøpende = internStatistikkService.finnAntallFagsakerLøpende(),
            antallBehandlingerIkkeFerdigstilt = internStatistikkService.finnAntallBehandlingerIkkeErAvsluttet(),
            antallBehandlingerPerÅrsak = internStatistikkService.finnAntallBehandlingerPerÅrsak(),
        )
        return ResponseEntity.ok(Ressurs.Companion.success(internstatistikk))
    }

    @GetMapping(path = ["antallSoknader"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentSøknadsstatistikkForPeriode(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fom: LocalDate?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tom: LocalDate?,
    ): ResponseEntity<Ressurs<SøknadsstatistikkForPeriode>> {
        val fomDato = fom ?: LocalDate.now().minusMonths(4).withDayOfMonth(1)
        val tomDato = tom ?: fomDato.plusMonths(4).minusDays(1)

        return RessursUtils.ok(behandlingSøknadsinfoService.hentSøknadsstatistikk(fomDato, tomDato))
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(InternStatistikkController::class.java)
    }
}

data class InternStatistikkResponse(
    val antallFagsakerTotalt: Long,
    val antallFagsakerLøpende: Long,
    val antallBehandlingerIkkeFerdigstilt: Long,
    val antallBehandlingerPerÅrsak: Map<BehandlingÅrsak, Long>,
)
