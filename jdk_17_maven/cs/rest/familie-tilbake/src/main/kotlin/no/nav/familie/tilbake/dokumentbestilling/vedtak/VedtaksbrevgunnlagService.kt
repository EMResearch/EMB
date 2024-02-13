package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevgunnlagService(private val vedtaksbrevgrunnlagRepository: VedtaksbrevgrunnlagRepository) {

    fun hentVedtaksbrevgrunnlag(behandlingId: UUID): Vedtaksbrevgrunnlag {
        val fagsakId = vedtaksbrevgrunnlagRepository.finnFagsakIdForBehandlingId(behandlingId)
        val vedtaksbrevgrunnlag = vedtaksbrevgrunnlagRepository.findByIdOrThrow(fagsakId)

        val originalBehandlingId =
            vedtaksbrevgrunnlag.behandlinger.first { it.id == behandlingId }.sisteÅrsak?.originalBehandlingId

        return vedtaksbrevgrunnlag.copy(
            behandlinger = vedtaksbrevgrunnlag.behandlinger.filter {
                it.id == behandlingId || it.id == originalBehandlingId
            }.toSet(),
        )
    }
}
