package no.nav.familie.ba.sak.kjerne.verge

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VergeRepository : JpaRepository<Verge, Long> {
    fun findByBehandling(behandling: Behandling): Verge?
}
