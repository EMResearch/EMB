package no.nav.familie.ba.sak.kjerne.eøs.felles

interface PeriodeOgBarnSkjemaEndringAbonnent<S : PeriodeOgBarnSkjema<S>> {
    fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<S>)
}
