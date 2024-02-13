package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ManuellBrevmottakerRepository :
    RepositoryInterface<ManuellBrevmottaker, UUID>,
    InsertUpdateRepository<ManuellBrevmottaker> {

    fun findByBehandlingId(behandlingId: UUID): List<ManuellBrevmottaker>
}
