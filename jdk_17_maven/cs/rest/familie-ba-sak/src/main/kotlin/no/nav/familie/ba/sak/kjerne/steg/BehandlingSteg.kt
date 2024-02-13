package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.steg.StegType.BEHANDLINGSRESULTAT
import no.nav.familie.ba.sak.kjerne.steg.StegType.BEHANDLING_AVSLUTTET
import no.nav.familie.ba.sak.kjerne.steg.StegType.BESLUTTE_VEDTAK
import no.nav.familie.ba.sak.kjerne.steg.StegType.DISTRIBUER_VEDTAKSBREV
import no.nav.familie.ba.sak.kjerne.steg.StegType.FERDIGSTILLE_BEHANDLING
import no.nav.familie.ba.sak.kjerne.steg.StegType.FILTRERING_FØDSELSHENDELSER
import no.nav.familie.ba.sak.kjerne.steg.StegType.HENLEGG_BEHANDLING
import no.nav.familie.ba.sak.kjerne.steg.StegType.IVERKSETT_MOT_FAMILIE_TILBAKE
import no.nav.familie.ba.sak.kjerne.steg.StegType.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ba.sak.kjerne.steg.StegType.JOURNALFØR_VEDTAKSBREV
import no.nav.familie.ba.sak.kjerne.steg.StegType.REGISTRERE_INSTITUSJON_OG_VERGE
import no.nav.familie.ba.sak.kjerne.steg.StegType.REGISTRERE_PERSONGRUNNLAG
import no.nav.familie.ba.sak.kjerne.steg.StegType.REGISTRERE_SØKNAD
import no.nav.familie.ba.sak.kjerne.steg.StegType.SEND_TIL_BESLUTTER
import no.nav.familie.ba.sak.kjerne.steg.StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI
import no.nav.familie.ba.sak.kjerne.steg.StegType.VILKÅRSVURDERING
import no.nav.familie.ba.sak.kjerne.steg.StegType.VURDER_TILBAKEKREVING

interface BehandlingSteg<T> {

    fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: T,
    ): StegType

    fun stegType(): StegType

    fun hentNesteStegForNormalFlyt(behandling: Behandling): StegType {
        return hentNesteSteg(
            utførendeStegType = this.stegType(),
            behandling = behandling,
            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
        )
    }

    fun hentNesteStegGittEndringerIUtbetaling(
        behandling: Behandling,
        endringerIUtbetaling: EndringerIUtbetalingForBehandlingSteg,
    ): StegType {
        return hentNesteSteg(
            utførendeStegType = this.stegType(),
            behandling = behandling,
            endringerIUtbetaling = endringerIUtbetaling,
        )
    }

    fun preValiderSteg(behandling: Behandling, stegService: StegService? = null) {}

    fun postValiderSteg(behandling: Behandling) {}
}

enum class EndringerIUtbetalingForBehandlingSteg {
    IKKE_RELEVANT,
    INGEN_ENDRING_I_UTBETALING,
    ENDRING_I_UTBETALING,
}

val FØRSTE_STEG = REGISTRERE_PERSONGRUNNLAG
val SISTE_STEG = BEHANDLING_AVSLUTTET

enum class StegType(
    val rekkefølge: Int,
    val tillattFor: List<BehandlerRolle>,
    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>,
) {

    // Henlegg søknad går utenfor den normale stegflyten og går direkte til ferdigstilt.
    // Denne typen av steg skal bli endret til å bli av type aksjonspunkt isteden for steg.
    HENLEGG_BEHANDLING(
        rekkefølge = 0,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(
            BehandlingStatus.UTREDES,
            BehandlingStatus.IVERKSETTER_VEDTAK,
        ),
    ),
    REGISTRERE_INSTITUSJON_OG_VERGE(
        rekkefølge = 1,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    REGISTRERE_PERSONGRUNNLAG(
        rekkefølge = 1,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    REGISTRERE_SØKNAD(
        rekkefølge = 1,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    FILTRERING_FØDSELSHENDELSER(
        rekkefølge = 2,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    VILKÅRSVURDERING(
        rekkefølge = 3,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BEHANDLINGSRESULTAT(
        rekkefølge = 4,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    VURDER_TILBAKEKREVING(
        rekkefølge = 5,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    SEND_TIL_BESLUTTER(
        rekkefølge = 6,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BESLUTTE_VEDTAK(
        rekkefølge = 7,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK),
    ),
    IVERKSETT_MOT_OPPDRAG(
        rekkefølge = 8,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(
        rekkefølge = 9,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    IVERKSETT_MOT_FAMILIE_TILBAKE(
        rekkefølge = 10,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    JOURNALFØR_VEDTAKSBREV(
        rekkefølge = 11,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    DISTRIBUER_VEDTAKSBREV(
        rekkefølge = 12,
        tillattFor = listOf(BehandlerRolle.SYSTEM),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    FERDIGSTILLE_BEHANDLING(
        rekkefølge = 13,
        tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigIKombinasjonMedStatus = listOf(
            BehandlingStatus.IVERKSETTER_VEDTAK,
            BehandlingStatus.UTREDES,
            BehandlingStatus.FATTER_VEDTAK,
        ),
    ),
    BEHANDLING_AVSLUTTET(
        rekkefølge = 14,
        tillattFor = emptyList(),
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.AVSLUTTET, BehandlingStatus.UTREDES),
    ),
    ;

    fun displayName(): String {
        return this.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    fun kommerEtter(steg: StegType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

    fun erSaksbehandlerSteg(): Boolean {
        return this.tillattFor.any { it == BehandlerRolle.SAKSBEHANDLER || it == BehandlerRolle.BESLUTTER }
    }
}

fun hentNesteSteg(
    behandling: Behandling,
    utførendeStegType: StegType,
    endringerIUtbetaling: EndringerIUtbetalingForBehandlingSteg = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
): StegType {
    if (utførendeStegType == HENLEGG_BEHANDLING) {
        return FERDIGSTILLE_BEHANDLING
    }

    val behandlingType = behandling.type
    val behandlingÅrsak = behandling.opprettetÅrsak

    if (behandlingÅrsak.erOmregningsårsak()) {
        return when (utførendeStegType) {
            REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
            VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
            BEHANDLINGSRESULTAT -> JOURNALFØR_VEDTAKSBREV
            JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
            DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
            FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
            BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
            else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
        }
    }

    return when (behandlingÅrsak) {
        BehandlingÅrsak.TEKNISK_OPPHØR -> throw Feil("Teknisk opphør er ikke mulig å behandle lenger")
        BehandlingÅrsak.MIGRERING -> throw Feil("Maskinell migrering er ikke mulig å behandle lenger")

        BehandlingÅrsak.HELMANUELL_MIGRERING -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> VURDER_TILBAKEKREVING
                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException(
                    "StegType ${utførendeStegType.displayName()} " +
                        "er ugyldig ved manuell migreringsbehandling",
                )
            }
        }

        BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> VURDER_TILBAKEKREVING
                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException(
                    "StegType ${utførendeStegType.displayName()} " +
                        "er ugyldig ved migreringsbehandling med endre migreringsdato",
                )
            }
        }

        BehandlingÅrsak.TEKNISK_ENDRING -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> VURDER_TILBAKEKREVING
                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentStegEtterBeslutteVedtakForTekniskEndring(endringerIUtbetaling)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("StegType ${utførendeStegType.displayName()} ugyldig ved teknisk endring")
            }
        }

        BehandlingÅrsak.FØDSELSHENDELSE -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> FILTRERING_FØDSELSHENDELSER
                FILTRERING_FØDSELSHENDELSER -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> if (endringerIUtbetaling == EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING) IVERKSETT_MOT_OPPDRAG else HENLEGG_BEHANDLING
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for fødselshendelser")
            }
        }

        BehandlingÅrsak.SØKNAD -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> {
                    if (behandling.fagsak.type == FagsakType.INSTITUSJON) {
                        REGISTRERE_INSTITUSJON_OG_VERGE
                    } else {
                        REGISTRERE_SØKNAD
                    }
                }

                REGISTRERE_INSTITUSJON_OG_VERGE -> REGISTRERE_SØKNAD
                REGISTRERE_SØKNAD -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> VURDER_TILBAKEKREVING
                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentNesteStegTypeBasertPåOmDetErEndringIUtbetaling(endringerIUtbetaling)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> IVERKSETT_MOT_FAMILIE_TILBAKE
                IVERKSETT_MOT_FAMILIE_TILBAKE -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }

        BehandlingÅrsak.SMÅBARNSTILLEGG -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> {
                    if (!behandling.skalBehandlesAutomatisk) {
                        VURDER_TILBAKEKREVING
                    } else if (behandling.skalBehandlesAutomatisk && behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) IVERKSETT_MOT_OPPDRAG else VURDER_TILBAKEKREVING
                }

                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentNesteStegTypeBasertPåOmDetErEndringIUtbetaling(endringerIUtbetaling)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> IVERKSETT_MOT_FAMILIE_TILBAKE
                IVERKSETT_MOT_FAMILIE_TILBAKE -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for småbarnstillegg")
            }
        }

        BehandlingÅrsak.SATSENDRING -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> if (endringerIUtbetaling == EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING) {
                    IVERKSETT_MOT_OPPDRAG
                } else if (behandling.kategori == BehandlingKategori.EØS && endringerIUtbetaling == EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING) {
                    FERDIGSTILLE_BEHANDLING
                } else {
                    throw Feil("Satsendringsbehandling har ingen endringer i utbetaling.")
                }

                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }

        else -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> BEHANDLINGSRESULTAT
                BEHANDLINGSRESULTAT -> VURDER_TILBAKEKREVING
                VURDER_TILBAKEKREVING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentNesteStegTypeBasertPåOmDetErEndringIUtbetaling(endringerIUtbetaling)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> IVERKSETT_MOT_FAMILIE_TILBAKE
                IVERKSETT_MOT_FAMILIE_TILBAKE -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }
    }
}

private fun hentNesteStegTypeBasertPåOmDetErEndringIUtbetaling(endringerIUtbetaling: EndringerIUtbetalingForBehandlingSteg): StegType =
    when (endringerIUtbetaling) {
        EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING -> IVERKSETT_MOT_OPPDRAG
        EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING -> JOURNALFØR_VEDTAKSBREV
        EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT -> throw Feil("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
    }

private fun hentStegEtterBeslutteVedtakForTekniskEndring(endringerIUtbetaling: EndringerIUtbetalingForBehandlingSteg): StegType =
    when (endringerIUtbetaling) {
        EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING -> IVERKSETT_MOT_OPPDRAG
        EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING -> FERDIGSTILLE_BEHANDLING
        EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT -> throw Feil("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
    }

enum class BehandlerRolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0),
}

enum class BehandlingStegStatus(val navn: String, val beskrivelse: String) {
    IKKE_UTFØRT("IKKE_UTFØRT", "Steget er ikke utført"),
    UTFØRT("UTFØRT", "Utført"),
}
