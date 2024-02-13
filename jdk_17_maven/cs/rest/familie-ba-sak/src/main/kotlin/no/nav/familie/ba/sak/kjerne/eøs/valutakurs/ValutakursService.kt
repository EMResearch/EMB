package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs>>,
) {
    val skjemaService = PeriodeOgBarnSkjemaService(
        valutakursRepository,
        endringsabonnenter,
    )

    fun hentValutakurs(valutakursId: Long): Valutakurs = skjemaService.hentMedId(valutakursId)

    fun hentValutakurser(behandlingId: BehandlingId) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterValutakurs(behandlingId: BehandlingId, valutakurs: Valutakurs) =
        skjemaService.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(behandlingId: BehandlingId, valutakursId: Long) =
        skjemaService.slettSkjema(behandlingId, valutakursId)

    @Transactional
    fun kopierOgErstattValutakurser(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) =
        skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
