package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import org.springframework.stereotype.Service

@Service
class EndretUtbetalingAndelHentOgPersisterService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
) {

    fun hentForBehandling(behandlingId: Long) = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)
}
