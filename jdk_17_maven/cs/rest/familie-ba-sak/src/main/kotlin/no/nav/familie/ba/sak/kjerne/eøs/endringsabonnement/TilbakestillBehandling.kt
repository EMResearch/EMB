package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingFraKompetanseEndringService(
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Kompetanse>) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}

@Service
class TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<UtenlandskPeriodebeløp>) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}

@Service
class TilbakestillBehandlingFraValutakursEndringService(
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Valutakurs>) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}
