package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface VedtaksbrevsoppsummeringRepository :
    RepositoryInterface<Vedtaksbrevsoppsummering, UUID>,
    InsertUpdateRepository<Vedtaksbrevsoppsummering> {

    fun findByBehandlingId(behandlingId: UUID): Vedtaksbrevsoppsummering?
}
