package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

object BehandlingsresultatUtils {

    internal fun skalUtledeSøknadsresultatForBehandling(behandling: Behandling): Boolean {
        return behandling.erManuellMigrering() || behandling.opprettetÅrsak in listOf(
            BehandlingÅrsak.SØKNAD,
            BehandlingÅrsak.FØDSELSHENDELSE,
            BehandlingÅrsak.KLAGE,
        )
    }

    internal fun kombinerResultaterTilBehandlingsresultat(
        søknadsresultat: Søknadsresultat?, // Søknadsresultat er null hvis det ikke er en søknad/fødselshendelse/manuell migrering
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
    ): Behandlingsresultat {
        fun sjekkResultat(
            ønsketSøknadsresultat: Søknadsresultat?,
            ønsketEndringsresultat: Endringsresultat,
            ønsketOpphørsresultat: Opphørsresultat,
        ): Boolean =
            søknadsresultat == ønsketSøknadsresultat && endringsresultat == ønsketEndringsresultat && opphørsresultat == ønsketOpphørsresultat

        fun ugyldigBehandlingsresultatFeil(behandlingsresultatString: String) =
            FunksjonellFeil(
                frontendFeilmelding = "Du har fått behandlingsresultatet $behandlingsresultatString, men behandlingen er registrert med årsak søknad. Du må enten innvilge eller avslå noe for å kunne fortsette. Om du er uenig i resultatet ta kontakt med Superbruker.",
                melding = "Kombinasjonen av (søknadsresultat=$søknadsresultat, endringsresultat=$endringsresultat, opphørsresultat=$opphørsresultat) er ikke støttet i løsningen.",
            )

        return when {
            // Søknad/fødselshendelse/manuell migrering
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Endret og opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Endret og fortsatt opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> throw ugyldigBehandlingsresultatFeil("Fortsatt opphørt")
            sjekkResultat(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.FORTSATT_INNVILGET

            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.INNVILGET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.INNVILGET
            sjekkResultat(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.INNVILGET

            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.AVSLÅTT_OG_ENDRET
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.AVSLÅTT_OG_ENDRET
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.AVSLÅTT
            sjekkResultat(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.AVSLÅTT

            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET
            sjekkResultat(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.DELVIS_INNVILGET

            // Ikke søknad/fødselshendelse/manuell migrering
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.ENDRET_OG_OPPHØRT
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.ENDRET_UTBETALING
            sjekkResultat(null, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.ENDRET_UTBETALING
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT) -> Behandlingsresultat.OPPHØRT
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT) -> Behandlingsresultat.FORTSATT_OPPHØRT
            sjekkResultat(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT) -> Behandlingsresultat.FORTSATT_INNVILGET

            // Skal egentlig aldri kunne komme hit, alle kombinasjoner skal være skrevet ut
            else -> throw Feil(
                frontendFeilmelding = "Du har fått et behandlingsresultat vi ikke støtter. Meld sak i Porten om du er uenig i resultatet.",
                message = "Klarer ikke utlede behandlingsresultat fra (søknadsresultat=$søknadsresultat, endringsresultat=$endringsresultat, opphørsresultat=$opphørsresultat)",
            )
        }
    }
}

fun hentUtbetalingstidslinjeForSøker(andeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): LocalDateTimeline<Int> {
    val utvidetTidslinje = LocalDateTimeline(
        andeler.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
            .map {
                LocalDateSegment(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned(),
                    it.kalkulertUtbetalingsbeløp,
                )
            },
    )
    val småbarnstilleggAndeler = LocalDateTimeline(
        andeler.filter { it.type == YtelseType.SMÅBARNSTILLEGG }.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it.kalkulertUtbetalingsbeløp,
            )
        },
    )

    return utvidetTidslinje.combine(
        småbarnstilleggAndeler,
        StandardCombinators::sum,
        LocalDateTimeline.JoinStyle.CROSS_JOIN,
    )
}
