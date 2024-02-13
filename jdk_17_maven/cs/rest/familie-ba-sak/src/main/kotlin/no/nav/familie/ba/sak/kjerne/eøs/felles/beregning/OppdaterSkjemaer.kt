package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.bareInnhold
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBarnOgPeriodeSomOverlapperMed

/**
 * Lager nye skjemaer der [oppdatering] overskriver skjemaet i [skjemaer]
 * som helt eller delvis overlapper. Hvis ingenting overlapper, så returneres [skjemaer]
 * @param[skjemaer]
 * @param[oppdatering]
 */
fun <T : PeriodeOgBarnSkjema<T>> oppdaterSkjemaerRekursivt(skjemaer: Collection<T>, oppdatering: T): Collection<T> {
    val førsteSkjemaSomOppdateres = skjemaer
        .filter { it.medBarnOgPeriodeSomOverlapperMed(oppdatering) != null } // Må overlappe i periode og barn
        .filter { it.bareInnhold() != oppdatering.bareInnhold() } // Må være en endring i selve innholdet i skjemaet
        .firstOrNull() ?: return skjemaer

    // oppdatertSkjema har innholdet fra oppdateringen, samt felles barn og perioder
    // Vi sjekket at det VAR en overlapp rett over, så det er ikke fare for NullPointerException
    val oppdatertSkjema = oppdatering.medBarnOgPeriodeSomOverlapperMed(førsteSkjemaSomOppdateres)!!

    // førsteSkjemaFratrukketOppdatering inneholder det som "blir igjen",
    // dvs det originale innholdet "utenfor" overlappende barn og periode
    val førsteSkjemaFratrukketOppdatering = førsteSkjemaSomOppdateres.trekkFra(oppdatertSkjema)

    val oppdaterteSkjemaer = skjemaer
        .minus(førsteSkjemaSomOppdateres)
        .plus(oppdatertSkjema)
        .plus(førsteSkjemaFratrukketOppdatering)
        .slåSammen()

    return oppdaterSkjemaerRekursivt(oppdaterteSkjemaer, oppdatering)
}
