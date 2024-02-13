package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/saksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(
    private val saksstatistikkService: SaksstatistikkService,
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) {

    private val logger = LoggerFactory.getLogger(SaksstatistikkController::class.java)

    @GetMapping(path = ["/behandling/{behandlingId}"])
    fun hentBehandlingDvh(@PathVariable(name = "behandlingId", required = true) behandlingId: Long): BehandlingDVH {
        try {
            return saksstatistikkService.mapTilBehandlingDVH(behandlingId)!!
        } catch (e: Exception) {
            logger.warn("Feil ved henting av sakstatistikk behandling", e)
            throw e
        }
    }

    @GetMapping(path = ["/sak/{fagsakId}"])
    fun hentSakDvh(@PathVariable(name = "fagsakId", required = true) fagsakId: Long): SakDVH {
        try {
            return saksstatistikkService.mapTilSakDvh(fagsakId)!!
        } catch (e: Exception) {
            logger.warn("Feil ved henting av sakstatistikk sak", e)
            throw e
        }
    }

    @Operation(
        description = "Oppdaterer saksstatistikk mellomlagring om at en melding har blitt sendt. Setter sendtTidspunkt slik at melding ikke blir sendt på nytt.",
    )
    @PostMapping(path = ["/registrer-sendt-fra-statistikk"])
    fun registrerSendtFraStatistikk(@RequestBody(required = true) input: SaksstatistikkSendtRequest): ResponseEntity<SaksstatistikkMellomlagring> {
        try {
            val jsnoNode = sakstatistikkObjectMapper.readTree(input.json)
            val funksjonellId = jsnoNode.get("funksjonellId").asText()
            val typeId = if (input.type == SaksstatistikkMellomlagringType.SAK) {
                jsnoNode.get("sakId").asLong()
            } else {
                jsnoNode.get("behandlingId").asLong()
            }
            val kontraktversjon = jsnoNode.get("versjon").asText()

            val sm = SaksstatistikkMellomlagring(
                offsetVerdiOnPrem = input.offset,
                funksjonellId = funksjonellId,
                type = input.type,
                json = input.json,
                typeId = typeId,
                kontraktVersjon = kontraktversjon,
                sendtTidspunkt = input.sendtTidspunkt,
            )

            saksstatistikkMellomlagringRepository.saveAndFlush(sm)
            return ResponseEntity.ok(sm)
        } catch (e: Exception) {
            logger.warn("Feil ved registrering av sendt", e)
            throw e
        }
    }

    data class SaksstatistikkSendtRequest(
        val offset: Long,
        val type: SaksstatistikkMellomlagringType,
        val json: String,
        val sendtTidspunkt: LocalDateTime,
    )
}
