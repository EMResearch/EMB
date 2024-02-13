package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class AutomatiskBeslutningServiceTest {
    @MockK
    private lateinit var simuleringService: SimuleringService

    @InjectMockKs
    private lateinit var automatiskBeslutningService: AutomatiskBeslutningService

    @Test
    fun `behandlingSkalAutomatiskBesluttes - skal returnere true dersom behandling er helmanuell migrering med avvik innenfor beløpsgrenser og det ikke finnes manuelle posteringer`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
        every { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) } returns true
        every { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) } returns false

        assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isTrue
    }

    @Test
    fun `behandlingSkalAutomatiskBesluttes - skal returnere true dersom behandling er endre migreringsdato behandling`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
        )

        assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isTrue
    }

    @Test
    fun `behandlingSkalAutomatiskBesluttes - skal returnere false dersom behandling er helmanuell migrering med avvik innenfor beløpsgrenser men det finnes manuelle posteringer`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
        every { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) } returns true
        every { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) } returns true

        assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isFalse
    }

    @Test
    fun `behandlingSkalAutomatiskBesluttes - skal returnere false dersom behandling er helmanuell migrering med avvik utenfor beløpsgrenser og det ikke finnes manuelle posteringer`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
        every { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) } returns false
        every { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) } returns false

        assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isFalse
    }

    @Test
    fun `behandlingSkalAutomatiskBesluttes - skal returnere false dersom behandling er helmanuell migrering med avvik utenfor beløpsgrenser og det finnes manuelle posteringer`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
        every { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) } returns false
        every { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) } returns true

        assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isFalse
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `behandlingSkalAutomatiskBesluttes - skal returnere false dersom behandling ikke er migrering uavhengig av avvik og manuelle posteringer`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        BehandlingType.values().filter {
            !listOf(
                BehandlingType.MIGRERING_FRA_INFOTRYGD,
                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
            ).contains(it)
        }.forEach { behandlingType ->
            val behandling = lagBehandling(
                behandlingType = behandlingType,
                årsak = behandlingÅrsak,
            )
            assertThat(automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)).isFalse
        }
    }
}
