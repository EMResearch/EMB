package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import org.springframework.data.jpa.repository.Query

interface ValutakursRepository : PeriodeOgBarnSkjemaRepository<Valutakurs> {

    @Query("SELECT vk FROM Valutakurs vk WHERE vk.behandlingId = :behandlingId")
    override fun finnFraBehandlingId(behandlingId: Long): List<Valutakurs>
}
