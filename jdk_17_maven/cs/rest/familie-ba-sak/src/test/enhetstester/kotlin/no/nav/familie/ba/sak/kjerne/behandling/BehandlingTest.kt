package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BehandlingTest {

    @Test
    fun `validerBehandling kaster feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk endring`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_ENDRING,
            årsak = BehandlingÅrsak.SØKNAD,
        )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster feil hvis behandlingType er teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.TEKNISK_OPPHØR,
        )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster feil hvis man prøver å opprette revurdering uten andre vedtatte behandlinger`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SØKNAD,
        )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster ikke feil hvis man prøver å opprette revurdering med andre vedtatte behandlinger`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SØKNAD,
        )
        assertDoesNotThrow {
            behandling.validerBehandlingstype(
                sisteBehandlingSomErVedtatt = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
            )
        }
    }

    @Test
    fun `erRentTekniskOpphør kastet feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.SØKNAD,
        )
        assertThrows<RuntimeException> { behandling.erTekniskOpphør() }
    }

    @Test
    fun `erRentTekniskOpphør gir true når teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.TEKNISK_OPPHØR,
        )
        assertTrue(behandling.erTekniskOpphør())
    }

    @Test
    fun `erRentTekniskOpphør gir false når ikke teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SØKNAD,
        )
        assertFalse(behandling.erTekniskOpphør())
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan sende vedtaksbrev for ordinær førstegangsbehandling`() {
        val behandling = lagBehandling()
        assertTrue { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan sende vedtaksbrev for ordinær revurdering`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        )
        assertTrue { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for migrering med endre migreringsdato`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
        )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for helmanuell migrering`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for automatisk migrering`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.MIGRERING,
        )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for teknisk endring`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_ENDRING,
        )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for revurdering med satsendring`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `Skal svare med overstyrt dokumenttittel på alle behandlinger som er definert som omgjøringsårsaker`() {
        BehandlingÅrsak.values().forEach {
            if (it.erOmregningsårsak()) {
                assertNotNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            } else {
                assertNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            }
        }
    }
}
