package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrevServiceTest {
    val saksbehandlerContext = mockk<SaksbehandlerContext>()
    val brevmalService = mockk<BrevmalService>()
    val brevService = BrevService(
        totrinnskontrollService = mockk(),
        persongrunnlagService = mockk(),
        arbeidsfordelingService = mockk(),
        simuleringService = mockk(),
        vedtaksperiodeService = mockk(),
        brevPeriodeService = mockk(),
        sanityService = mockk(),
        vilkårsvurderingService = mockk(),
        korrigertEtterbetalingService = mockk(),
        organisasjonService = mockk(),
        korrigertVedtakService = mockk(),
        saksbehandlerContext = saksbehandlerContext,
        brevmalService = brevmalService,
        refusjonEøsRepository = mockk(),
        unleashNext = mockk(),
        integrasjonClient = mockk(),
    )

    @BeforeEach
    fun setUp() {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"
    }

    @Test
    fun `Saksbehandler blir hentet fra sikkerhetscontext og beslutter viser placeholder tekst under behandling`() {
        val behandling = lagBehandling()

        val (saksbehandler, beslutter) = brevService.hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = null,
        )

        Assertions.assertEquals("saksbehandlerNavn", saksbehandler)
        Assertions.assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter er hentet fra sikkerhetscontext under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = brevService.hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "mock.saksbehandler@nav.no",
            ),
        )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("saksbehandlerNavn", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter viser placeholder tekst under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = brevService.hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "System",
                saksbehandlerId = "systembruker",
            ),
        )

        Assertions.assertEquals("System", saksbehandler)
        Assertions.assertEquals("saksbehandlerNavn", beslutter)
    }

    @Test
    fun `Saksbehandler og beslutter blir hentet etter at totrinnskontroll er besluttet`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = brevService.hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "mock.saksbehandler@nav.no",
                beslutter = "Mock Beslutter",
                beslutterId = "mock.beslutter@nav.no",
            ),
        )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("Mock Beslutter", beslutter)
    }
}
