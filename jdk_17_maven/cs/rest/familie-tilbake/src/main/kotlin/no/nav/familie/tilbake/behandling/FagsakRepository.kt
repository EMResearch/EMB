package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak> {

    @Query("""SELECT f.* FROM fagsak f JOIN behandling b ON b.fagsak_id = f.id WHERE b.id = :behandlingId""")
    fun finnFagsakForBehandlingId(behandlingId: UUID): Fagsak

    @Query("""SELECT f.* FROM fagsak f JOIN behandling b ON b.fagsak_id = f.id WHERE b.ekstern_bruk_id = :eksternBrukId""")
    fun finnFagsakForEksternBrukId(eksternBrukId: UUID): Fagsak

    fun findByFagsystemAndEksternFagsakId(fagsystem: Fagsystem, eksternFagsakId: String): Fagsak?

    @Query("""SELECT f.* FROM fagsak f WHERE f.fagsystem = :fagsystem AND f.bruker_ident=:personIdent""")
    fun finnFagsakForFagsystemAndIdent(fagsystem: Fagsystem, personIdent: String): Fagsak?
}
