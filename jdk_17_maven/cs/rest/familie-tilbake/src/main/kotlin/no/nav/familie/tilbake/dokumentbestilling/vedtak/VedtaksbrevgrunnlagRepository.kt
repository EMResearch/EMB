package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.intellij.lang.annotations.Language
import org.springframework.data.jdbc.repository.query.Query
import java.util.UUID

interface VedtaksbrevgrunnlagRepository :
    RepositoryInterface<Vedtaksbrevgrunnlag, UUID>,
    InsertUpdateRepository<Vedtaksbrevgrunnlag> {

    @Language("PostgreSQL")
    @Query("SELECT fagsak_id FROM behandling WHERE id = :behandlingId")
    fun finnFagsakIdForBehandlingId(behandlingId: UUID): UUID
}
