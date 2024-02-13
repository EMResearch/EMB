package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.common.feilHvis
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.oppdaterSkjemaerRekursivt
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.somInversOppdateringEllersNull

class PeriodeOgBarnSkjemaService<S : PeriodeOgBarnSkjemaEntitet<S>>(
    val periodeOgBarnSkjemaRepository: PeriodeOgBarnSkjemaRepository<S>,
    val endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<S>>,
) {

    fun hentMedBehandlingId(behandlingId: BehandlingId): Collection<S> {
        return periodeOgBarnSkjemaRepository.finnFraBehandlingId(behandlingId.id)
    }

    fun hentMedId(id: Long): S {
        return periodeOgBarnSkjemaRepository.getById(id)
    }

    fun endreSkjemaer(behandlingId: BehandlingId, oppdatering: S) {
        val gjeldendeSkjemaer = hentMedBehandlingId(behandlingId)

        val justertOppdatering = oppdatering.somInversOppdateringEllersNull(gjeldendeSkjemaer) ?: oppdatering
        val oppdaterteKompetanser = oppdaterSkjemaerRekursivt(gjeldendeSkjemaer, justertOppdatering)

        lagreDifferanseOgVarsleAbonnenter(
            behandlingId,
            gjeldendeSkjemaer,
            oppdaterteKompetanser.medBehandlingId(behandlingId),
        )
    }

    fun slettSkjema(behandlingId: BehandlingId, skjemaId: Long) {
        val skjemaTilSletting = periodeOgBarnSkjemaRepository.getById(skjemaId)
        feilHvis(skjemaTilSletting.behandlingId != behandlingId.id) {
            "Prøver å slette et skjema som ikke er koblet til behandlingen man sender inn"
        }
        val gjeldendeSkjemaer = hentMedBehandlingId(behandlingId)
        val blanktSkjema = skjemaTilSletting.utenInnhold()

        val oppdaterteKompetanser = gjeldendeSkjemaer.minus(skjemaTilSletting).plus(blanktSkjema)
            .slåSammen().medBehandlingId(behandlingId)

        lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeSkjemaer, oppdaterteKompetanser)
    }

    fun kopierOgErstattSkjemaer(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val gjeldendeTilSkjemaer = hentMedBehandlingId(tilBehandlingId)
        val kopiAvFraSkjemaer = hentMedBehandlingId(fraBehandlingId)
            .map { it.kopier() }
            .medBehandlingId(tilBehandlingId)

        periodeOgBarnSkjemaRepository.deleteAll(gjeldendeTilSkjemaer)
        periodeOgBarnSkjemaRepository.saveAll(kopiAvFraSkjemaer)
    }

    fun lagreDifferanseOgVarsleAbonnenter(
        behandlingId: BehandlingId,
        gjeldende: Collection<S>,
        oppdaterte: Collection<S>,
    ) {
        val skalSlettes = gjeldende - oppdaterte
        val skalLagres = oppdaterte - gjeldende

        periodeOgBarnSkjemaRepository.deleteAll(skalSlettes)
        periodeOgBarnSkjemaRepository.saveAll(skalLagres)

        val endringer = skalSlettes + skalLagres
        if (endringer.isNotEmpty()) {
            endringsabonnenter.forEach { it.skjemaerEndret(behandlingId, oppdaterte) }
        }
    }
}

fun <T : PeriodeOgBarnSkjemaEntitet<T>> Collection<T>.medBehandlingId(behandlingId: BehandlingId): Collection<T> {
    this.forEach { it.behandlingId = behandlingId.id }
    return this
}
