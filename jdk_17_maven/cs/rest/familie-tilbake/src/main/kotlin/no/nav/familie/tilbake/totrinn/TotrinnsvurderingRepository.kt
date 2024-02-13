package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface TotrinnsvurderingRepository : RepositoryInterface<Totrinnsvurdering, UUID>, InsertUpdateRepository<Totrinnsvurdering> {

    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): List<Totrinnsvurdering>
}
