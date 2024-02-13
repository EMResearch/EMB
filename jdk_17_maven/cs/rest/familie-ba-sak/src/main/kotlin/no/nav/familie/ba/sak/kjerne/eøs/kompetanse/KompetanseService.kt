package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>,
) {
    val skjemaService = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        endringsabonnenter,
    )

    fun hentKompetanser(behandlingId: BehandlingId) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun hentKompetanse(kompetanseId: Long) =
        skjemaService.hentMedId(kompetanseId)

    @Transactional
    fun oppdaterKompetanse(behandlingId: BehandlingId, oppdatering: Kompetanse) =
        skjemaService.endreSkjemaer(behandlingId, oppdatering)

    @Transactional
    fun slettKompetanse(behandlingId: BehandlingId, kompetanseId: Long) =
        skjemaService.slettSkjema(behandlingId, kompetanseId)

    @Transactional
    fun kopierOgErstattKompetanser(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) =
        skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
