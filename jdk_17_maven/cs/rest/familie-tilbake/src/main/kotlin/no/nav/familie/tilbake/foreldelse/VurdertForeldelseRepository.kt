package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VurdertForeldelseRepository : RepositoryInterface<VurdertForeldelse, UUID>, InsertUpdateRepository<VurdertForeldelse> {

    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): VurdertForeldelse?
}
