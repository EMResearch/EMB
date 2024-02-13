package no.nav.familie.ba.sak.kjerne.totrinnskontroll

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TotrinnskontrollTest(
    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `Skal godkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        )
            .hasSize(2)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            )
                .last().jsonToBehandlingDVH().behandlingStatus,
        ).isEqualTo(BehandlingStatus.FATTER_VEDTAK.name)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)

        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter", "beslutterId", Beslutning.GODKJENT)

        assertEquals(BehandlingStatus.IVERKSETTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)

        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        )
            .hasSize(3)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            )
                .last().jsonToBehandlingDVH().behandlingStatus,
        ).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK.name)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertTrue(totrinnskontroll.godkjent)
    }

    @Test
    @Tag("integration")
    fun `Skal underkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter", "beslutterId", Beslutning.UNDERKJENT)
        assertEquals(BehandlingStatus.UTREDES, behandlingHentOgPersisterService.hent(behandling.id).status)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        )
            .hasSize(3)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            )
                .last().jsonToBehandlingDVH().behandlingStatus,
        ).isEqualTo(BehandlingStatus.UTREDES.name)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertFalse(totrinnskontroll.godkjent)
    }

    @Test
    fun `Skal ikke kunne godkjenne eget vedtak`() {
        val totrinnskontroll = Totrinnskontroll(
            behandling = lagBehandling(),
            saksbehandler = "Mock Saksbehandler",
            saksbehandlerId = "Mock.Saksbehandler",
            beslutter = "Mock Saksbehandler",
            beslutterId = "Mock.Saksbehandler",
            godkjent = true,
        )

        assertTrue(totrinnskontroll.erUgyldig())
    }

    @Test
    fun `Skal kunne underkjenne eget vedtak`() {
        val totrinnskontroll = Totrinnskontroll(
            behandling = lagBehandling(),
            saksbehandler = "Mock Saksbehandler",
            saksbehandlerId = "Mock.Saksbehandler",
            beslutter = "Mock Saksbehandler",
            beslutterId = "Mock.Saksbehandler",
            godkjent = false,
        )

        assertFalse(totrinnskontroll.erUgyldig())
    }
}
