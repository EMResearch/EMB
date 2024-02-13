package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingStegControllerTest {

    private val featureToggleServiceMock = mockk<FeatureToggleService>()
    private val tilgangServiceMock = mockk<TilgangService>()
    private val behandlingHentOgPersisterServiceMock = mockk<BehandlingHentOgPersisterService>()
    private val stegServiceMock = mockk<StegService>()
    private val utvidetBehandlingServiceMock = mockk<UtvidetBehandlingService>()
    private val behandlingStegController = BehandlingStegController(
        behandlingHentOgPersisterService = behandlingHentOgPersisterServiceMock,
        stegService = stegServiceMock,
        tilgangService = tilgangServiceMock,
        featureToggleService = featureToggleServiceMock,
        utvidetBehandlingService = utvidetBehandlingServiceMock,
    )

    @Test
    fun `Skal kaste feil hvis saksbehandler uten teknisk endring-tilgang prøver å henlegge en behandling med årsak=teknisk endring`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

        every { featureToggleServiceMock.isEnabled(FeatureToggleConfig.TEKNISK_ENDRING) } returns false
        every { featureToggleServiceMock.isEnabled(FeatureToggleConfig.TEKNISK_VEDLIKEHOLD_HENLEGGELSE) } returns false
        every { tilgangServiceMock.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        every { stegServiceMock.håndterHenleggBehandling(any(), any()) } returns behandling

        assertThrows<FunksjonellFeil> {
            behandlingStegController.henleggBehandlingOgSendBrev(
                behandlingId = behandling.id,
                henleggInfo = RestHenleggBehandlingInfo(
                    begrunnelse = "dette er en begrunnelse",
                    årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                ),
            )
        }
    }
}
