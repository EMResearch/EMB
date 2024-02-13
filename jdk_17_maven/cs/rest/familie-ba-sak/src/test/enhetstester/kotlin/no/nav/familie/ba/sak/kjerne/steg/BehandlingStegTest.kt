package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på behandling ENDRE_MIGRERINGSDATO`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                ),
                utførendeStegType = it,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av søknad ved endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.REGISTRERE_SØKNAD,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av søknad ved ingen endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.REGISTRERE_SØKNAD,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av fødselshendelser ved endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.FILTRERING_FØDSELSHENDELSER,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av fødselshendelser ved ingen endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.FILTRERING_FØDSELSHENDELSER,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.HENLEGG_BEHANDLING,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester at neste steg for migrering fra infotrygd kaster feil at denne ikke er mulig å behandle lenger`() {
        val feil = assertThrows<Feil> {
            hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.MIGRERING,
                ),
                utførendeStegType = FØRSTE_STEG,
            )
        }
        assertEquals("Maskinell migrering er ikke mulig å behandle lenger", feil.message)
    }

    @Test
    fun `Tester rekkefølgen på behandling av type teknisk endring ved endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            assertNotEquals(StegType.JOURNALFØR_VEDTAKSBREV, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    årsak = BehandlingÅrsak.TEKNISK_ENDRING,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `teknisk endring skal ha riktig seg ved behandlingsresultat fortsatt innvilget ved ingen endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(steg, it)
            assertNotEquals(StegType.JOURNALFØR_VEDTAKSBREV, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    årsak = BehandlingÅrsak.TEKNISK_ENDRING,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av omregn 18 år`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.OMREGNING_18ÅR,
                ),
                utførendeStegType = it,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen til manuell behandling med årsak småbarnstillegg ved endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av ÅRLIG_KONTROLL, som er test av else gren ved endring i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av satsendring ved endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.SATSENDRING,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Tester at man ikke får lov til å komme videre etter behandlingsresultat dersom det er ingen endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
        ).forEach {
            assertEquals(it, steg)
            if (it == StegType.BEHANDLINGSRESULTAT) {
                assertThrows<Feil> {
                    hentNesteSteg(
                        behandling = lagBehandling(
                            årsak = BehandlingÅrsak.SATSENDRING,
                        ),
                        utførendeStegType = it,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )
                }
            } else {
                steg = hentNesteSteg(
                    behandling = lagBehandling(
                        årsak = BehandlingÅrsak.SATSENDRING,
                    ),
                    utførendeStegType = it,
                    endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                )
            }
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av ÅRLIG_KONTROLL, som er test av else gren, med FORTSATT_INNVILGET ved ingen endringer i utbetaling`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET,
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ),
                utførendeStegType = it,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
            )
        }
    }

    @Test
    fun `Skal kaste feil dersom det er en søknad og det forsøkes å gå videre fra beslutt vedtak uten at det har vært sjekk om det finnes endringer i utbetaling`() {
        assertThrows<Feil> {
            hentNesteSteg(
                lagBehandling(
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
                utførendeStegType = StegType.BESLUTTE_VEDTAK,
                endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
            )
        }
    }

    @Test
    fun testDisplayName() {
        assertEquals("Send til beslutter", StegType.SEND_TIL_BESLUTTER.displayName())
    }

    @Test
    fun testErKompatibelMed() {
        assertTrue(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertFalse(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertFalse(StegType.BEHANDLING_AVSLUTTET.erGyldigIKombinasjonMedStatus(BehandlingStatus.FATTER_VEDTAK))
    }
}
