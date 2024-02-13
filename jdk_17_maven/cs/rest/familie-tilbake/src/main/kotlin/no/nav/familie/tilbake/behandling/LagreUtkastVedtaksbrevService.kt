package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LagreUtkastVedtaksbrevService(
    val behandlingRepository: BehandlingRepository,
    val behandlingskontrollService: BehandlingskontrollService,
    val vedtaksbrevService: VedtaksbrevService,
) {

    fun lagreUtkast(behandlingId: UUID, fritekstavsnitt: FritekstavsnittDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet")
        }
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(
                message = "Behandling med id=$behandlingId er på vent, kan ikke lagre utkast av vedtaksbrevet",
                frontendFeilmelding = "Behandling med id=$behandlingId er på vent, kan ikke lagre utkast av vedtaksbrevet",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        vedtaksbrevService.lagreUtkastAvFritekster(behandlingId, fritekstavsnitt)
    }
}
