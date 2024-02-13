package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingsstegstilstandRepository :
    RepositoryInterface<Behandlingsstegstilstand, UUID>,
    InsertUpdateRepository<Behandlingsstegstilstand> {

    fun findByBehandlingId(behandlingId: UUID): List<Behandlingsstegstilstand>

    fun findByBehandlingIdAndBehandlingsstegsstatusIn(
        behandlingId: UUID,
        statuser: List<Behandlingsstegstatus>,
    ): Behandlingsstegstilstand?

    fun findByBehandlingIdAndBehandlingssteg(behandlingId: UUID, behandlingssteg: Behandlingssteg): Behandlingsstegstilstand?
}
