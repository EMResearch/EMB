package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FaktaFeilutbetalingRepository :
    RepositoryInterface<FaktaFeilutbetaling, UUID>,
    InsertUpdateRepository<FaktaFeilutbetaling> {

    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): FaktaFeilutbetaling?

    fun findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId: UUID): FaktaFeilutbetaling
}
