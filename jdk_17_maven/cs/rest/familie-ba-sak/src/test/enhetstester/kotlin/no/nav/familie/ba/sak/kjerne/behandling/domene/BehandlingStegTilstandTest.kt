package no.nav.familie.ba.sak.kjerne.behandling.domene

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BehandlingStegTilstandTest {

    @Test
    fun `Verifiser at siste steg får status IKKE_UTFØRT`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.IKKE_UTFØRT,
            behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus,
        )
    }

    @Test
    fun `Verifiser maks et steg av hver type`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        assertEquals(
            BehandlingStegStatus.IKKE_UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus,
        )
    }

    @Test
    fun `Verifiser at alle steg med høyere rekkefølge enn siste fjernes`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.IKKE_UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus,
        )
        assertTrue(behandling.behandlingStegTilstand.none { it.behandlingSteg == StegType.SEND_TIL_BESLUTTER })
    }

    @Test
    fun `Verifiser henlegg søknad ikke endrer stegstatus`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.leggTilHenleggStegOmDetIkkeFinnesFraFør()

        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.IKKE_UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.SEND_TIL_BESLUTTER }.behandlingStegStatus,
        )
        assertEquals(
            BehandlingStegStatus.IKKE_UTFØRT,
            behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.HENLEGG_BEHANDLING }.behandlingStegStatus,
        )
    }

    fun opprettBehandling(): Behandling {
        return Behandling(
            id = 1,
            fagsak = mockk(),
            kategori = BehandlingKategori.NASJONAL,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD,
        ).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, StegType.REGISTRERE_SØKNAD))
        }
    }
}
