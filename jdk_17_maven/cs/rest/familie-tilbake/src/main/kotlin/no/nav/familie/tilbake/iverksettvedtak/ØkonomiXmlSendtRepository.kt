package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlSendtRepository : RepositoryInterface<ØkonomiXmlSendt, UUID>, InsertUpdateRepository<ØkonomiXmlSendt> {

    fun findByBehandlingId(behandlingId: UUID): ØkonomiXmlSendt?

    // language=PostgreSQL
    @Query("SELECT * FROM okonomi_xml_sendt WHERE opprettet_tid::DATE = :opprettetTid ")
    fun findByOpprettetPåDato(opprettetTid: LocalDate): Collection<ØkonomiXmlSendt>
}
