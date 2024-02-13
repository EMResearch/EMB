package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraBarn
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraBarnOgTilOgMed
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraTilOgMed
import no.nav.familie.ba.sak.kjerne.eøs.felles.harEkteDelmengdeAvBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBarnaSomForsvinnerFra
import no.nav.familie.ba.sak.kjerne.eøs.felles.tilOgMedBlirForkortetEllerLukketAv
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenInnholdTilOgMed

/**
 * Funksjon som inverterer en skjema-oppdatering,[this], som skal endre et sett av [gjeldendeSkjemaer]
 *
 * I tilfellet der oppdateringen:
 * 1. Gjelder ett gjeldende skjema
 * 2a. Lukker periode på det gjeldende skjemaet, dvs til-og-med går fra <null> til en verdi, eller til-og-med er tidligere
 * 2b. og/eller reduserer antall barn
 * så skal det lages en ny oppdatering med blankt skjema for det som ligger "utenfor" [oppdatering], dvs har
 * 1. Perioden som starter måneden etter ny til-og-med-dato, og frem frem til eksisterende til-og-med (kan være <null>)
 * 2. Barnet/barna som blir fjernet
 *
 * Problemet som skal løses er at skjemaer som kun varierer i periode eller barn, slås sammen fordi de ellers er like
 * Lukking/forkorting av periode eller fjerning av barn vil føre til en umiddelbar sammenslåing og nulle ut oppdateringen
 * Ved å lage den "motsatte" endringen med et tomt skjema "utenfor" det gjeldende skjemaet,
 * blir nettoeffekten at den ønskede oppdateringen oppstår, og et tomt skjema dekker området rundt
 *
 */
fun <T : PeriodeOgBarnSkjemaEntitet<T>> T.somInversOppdateringEllersNull(gjeldendeSkjemaer: Collection<T>): T? {
    val oppdatering = this

    val skjemaetDerTilOgMedForkortes = gjeldendeSkjemaer.filter { gjeldende ->
        gjeldende.tilOgMedBlirForkortetEllerLukketAv(oppdatering) &&
            gjeldende.erLikBortsettFraTilOgMed(oppdatering)
    }.singleOrNull()

    val skjemaetDerBarnFjernes = gjeldendeSkjemaer.filter { gjeldende ->
        oppdatering.harEkteDelmengdeAvBarna(gjeldende) &&
            gjeldende.erLikBortsettFraBarn(oppdatering)
    }.singleOrNull()

    val skjemaetDerTilOgMedForkortesOgBarnFjernes = gjeldendeSkjemaer.filter { gjeldende ->
        gjeldende.tilOgMedBlirForkortetEllerLukketAv(oppdatering) &&
            oppdatering.harEkteDelmengdeAvBarna(gjeldende) &&
            gjeldende.erLikBortsettFraBarnOgTilOgMed(oppdatering)
    }.singleOrNull()

    return when {
        skjemaetDerTilOgMedForkortesOgBarnFjernes != null ->
            oppdatering.medBarnaSomForsvinnerFra(skjemaetDerTilOgMedForkortesOgBarnFjernes)
                .utenInnholdTilOgMed(skjemaetDerTilOgMedForkortesOgBarnFjernes.tom)
        skjemaetDerBarnFjernes != null ->
            oppdatering.medBarnaSomForsvinnerFra(skjemaetDerBarnFjernes).utenInnhold()
        skjemaetDerTilOgMedForkortes != null ->
            oppdatering.utenInnholdTilOgMed(skjemaetDerTilOgMedForkortes.tom)
        else -> null
    }
}
