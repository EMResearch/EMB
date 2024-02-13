package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import org.springframework.stereotype.Service

@Service
class BrevmalService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    fun hentBrevmal(behandling: Behandling): Brevmal =
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.DØDSFALL_BRUKER -> Brevmal.VEDTAK_OPPHØR_DØDSFALL
            BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV
            else -> hentVedtaksbrevmal(behandling)
        }

    fun hentVedtaksbrevmal(behandling: Behandling): Brevmal {
        if (behandling.resultat == Behandlingsresultat.IKKE_VURDERT) {
            throw Feil("Kan ikke opprette brev. Behandlingen er ikke vurdert.")
        }

        val brevmal = if (behandling.skalBehandlesAutomatisk) {
            hentAutomatiskVedtaksbrevtype(behandling)
        } else {
            hentManuellVedtaksbrevtype(behandling)
        }

        return if (brevmal.erVedtaksbrev) brevmal else throw Feil("Brevmal ${brevmal.visningsTekst} er ikke vedtaksbrev")
    }

    fun hentManuellVedtaksbrevtype(
        behandling: Behandling,
    ): Brevmal {
        val behandlingType = behandling.type
        val behandlingsresultat = behandling.resultat
        val erInstitusjon = behandling.fagsak.institusjon != null
        val ytelseErLøpende by lazy {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
                .any { it.erLøpende() }
        }

        val feilmeldingBehandlingTypeOgResultat =
            "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingsresultat"
        val feilmelidingBehandlingType =
            "Brev ikke støttet for behandlingstype=$behandlingType"
        val frontendFeilmelding =
            "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
                "Meld sak i Porten slik at vi kan se nærmere på saken."

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                if (erInstitusjon) {
                    when (behandlingsresultat) {
                        Behandlingsresultat.INNVILGET,
                        Behandlingsresultat.INNVILGET_OG_ENDRET,
                        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                        -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON

                        Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG_INSTITUSJON

                        Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_OPPHØRT,
                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
                        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        Behandlingsresultat.IKKE_VURDERT,
                        -> throw FunksjonellFeil(
                            melding = feilmeldingBehandlingTypeOgResultat,
                            frontendFeilmelding = frontendFeilmelding,
                        )
                    }
                } else {
                    when (behandlingsresultat) {
                        Behandlingsresultat.INNVILGET,
                        Behandlingsresultat.INNVILGET_OG_ENDRET,
                        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                        -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

                        Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                        Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_OPPHØRT,
                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
                        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        Behandlingsresultat.IKKE_VURDERT,
                        -> throw FunksjonellFeil(
                            melding = feilmeldingBehandlingTypeOgResultat,
                            frontendFeilmelding = frontendFeilmelding,
                        )
                    }
                }

            BehandlingType.REVURDERING ->
                if (erInstitusjon) {
                    when (behandlingsresultat) {
                        Behandlingsresultat.INNVILGET,
                        Behandlingsresultat.INNVILGET_OG_ENDRET,
                        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        -> if (ytelseErLøpende) Brevmal.VEDTAK_ENDRING_INSTITUSJON else Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON

                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_OPPHØRT,
                        -> Brevmal.VEDTAK_OPPHØRT_INSTITUSJON

                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET,
                        -> Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON

                        Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG_INSTITUSJON

                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
                        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        Behandlingsresultat.IKKE_VURDERT,
                        -> throw FunksjonellFeil(
                            melding = feilmeldingBehandlingTypeOgResultat,
                            frontendFeilmelding = frontendFeilmelding,
                        )
                    }
                } else {
                    when (behandlingsresultat) {
                        Behandlingsresultat.INNVILGET,
                        Behandlingsresultat.INNVILGET_OG_ENDRET,
                        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        -> if (ytelseErLøpende) Brevmal.VEDTAK_ENDRING else Brevmal.VEDTAK_OPPHØR_MED_ENDRING

                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_OPPHØRT,
                        -> Brevmal.VEDTAK_OPPHØRT

                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET,
                        -> Brevmal.VEDTAK_FORTSATT_INNVILGET

                        Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG
                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
                        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        Behandlingsresultat.IKKE_VURDERT,
                        -> throw FunksjonellFeil(
                            melding = feilmeldingBehandlingTypeOgResultat,
                            frontendFeilmelding = frontendFeilmelding,
                        )
                    }
                }

            BehandlingType.MIGRERING_FRA_INFOTRYGD,
            BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
            BehandlingType.TEKNISK_OPPHØR,
            BehandlingType.TEKNISK_ENDRING,
            -> throw FunksjonellFeil(
                melding = feilmelidingBehandlingType,
                frontendFeilmelding = frontendFeilmelding,
            )
        }
    }
}
