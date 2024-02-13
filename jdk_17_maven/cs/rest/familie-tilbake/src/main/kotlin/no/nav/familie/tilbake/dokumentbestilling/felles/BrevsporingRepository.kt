package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface BrevsporingRepository : RepositoryInterface<Brevsporing, UUID>, InsertUpdateRepository<Brevsporing> {

    fun findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandlingId: UUID, brevtype: Brevtype): Brevsporing?

    fun existsByBehandlingIdAndBrevtypeIn(behandlingId: UUID, brevtype: Set<Brevtype>): Boolean

    fun existsByBehandlingId(behandlingId: UUID): Boolean
}
