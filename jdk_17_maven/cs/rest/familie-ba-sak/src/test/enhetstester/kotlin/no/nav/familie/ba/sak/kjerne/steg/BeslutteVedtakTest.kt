package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.task.FerdigstillOppgaver
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BeslutteVedtakTest {

    private lateinit var beslutteVedtak: BeslutteVedtak
    private lateinit var vedtakService: VedtakService
    private lateinit var behandlingService: BehandlingService
    private lateinit var beregningService: BeregningService
    private lateinit var taskRepository: TaskRepositoryWrapper
    private lateinit var dokumentService: DokumentService
    private lateinit var vilkårsvurderingService: VilkårsvurderingService
    private lateinit var featureToggleService: FeatureToggleService
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService
    private lateinit var simuleringService: SimuleringService
    private lateinit var automatiskBeslutningService: AutomatiskBeslutningService

    private val saksbehandlerContext = mockk<SaksbehandlerContext>()

    private val randomVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

    @BeforeEach
    fun setUp() {
        val toTrinnKontrollService = mockk<TotrinnskontrollService>()
        vedtakService = mockk()
        taskRepository = mockk()
        dokumentService = mockk()
        behandlingService = mockk()
        beregningService = mockk()
        vilkårsvurderingService = mockk()
        featureToggleService = mockk()
        tilkjentYtelseValideringService = mockk()
        simuleringService = mockk()
        automatiskBeslutningService = mockk()

        val loggService = mockk<LoggService>()

        every { taskRepository.save(any()) } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        every {
            toTrinnKontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Totrinnskontroll(
            behandling = lagBehandling(),
            saksbehandler = "Mock Saksbehandler",
            saksbehandlerId = "Mock.Saksbehandler",
        )
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any(), any()) } just Runs
        every { vedtakService.oppdaterVedtaksdatoOgBrev(any()) } just runs
        every { behandlingService.opprettOgInitierNyttVedtakForBehandling(any(), any(), any()) } just runs
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns randomVilkårsvurdering
        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) } returns randomVilkårsvurdering
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"

        beslutteVedtak = BeslutteVedtak(
            toTrinnKontrollService,
            vedtakService,
            behandlingService,
            beregningService,
            taskRepository,
            loggService,
            vilkårsvurderingService,
            featureToggleService,
            tilkjentYtelseValideringService,
            saksbehandlerContext,
            automatiskBeslutningService,
        )
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave ved Godkjent vedtak`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
        mockkObject(FerdigstillOppgaver.Companion)
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
        every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        Assertions.assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, nesteSteg)
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave og opprette Behandle Underkjent Vedtak-oppgave ved Underkjent vedtak`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.UNDERKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgaver.Companion)
        mockkObject(OpprettOppgaveTask.Companion)
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
        every {
            OpprettOppgaveTask.opprettTask(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        verify(exactly = 1) {
            OpprettOppgaveTask.opprettTask(
                behandling.id,
                Oppgavetype.BehandleUnderkjentVedtak,
                any(),
                any(),
                any(),
            )
        }
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, nesteSteg)
    }

    @Test
    fun `Skal ikke iverksette hvis det ikke er forskjell i utbetaling mellom nåværende og forrige andeler`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling)
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING

        mockkObject(JournalførVedtaksbrevTask.Companion)
        every {
            JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                any(),
                any(),
                any(),
            )
        } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) }

        verify(exactly = 1) {
            JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingId = behandling.id,
                vedtakId = vedtak.id,
            )
        }
        Assertions.assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, nesteSteg)
    }

    @Test
    fun `Skal initiere nytt vedtak når vedtak ikke er godkjent`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.UNDERKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgaver.Companion)
        mockkObject(OpprettOppgaveTask.Companion)
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task(
            OpprettOppgaveTask.TASK_STEP_TYPE,
            "",
        )

        every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

        beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)
        verify(exactly = 1) { behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling, true, emptyList()) }
    }

    @Test
    fun `Skal kaste feil dersom toggle ikke er enabled og årsak er korreksjon vedtaksbrev`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) } returns false

        val behandling = lagBehandling(årsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgaver.Companion)
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(
            type = FerdigstillOppgaver.TASK_STEP_TYPE,
            payload = "",
        )

        assertThrows<FunksjonellFeil> { beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak) }
    }

    @Test
    fun `Skal kaste feil dersom saksbehandler uten tilgang til teknisk endring prøve å godkjenne en behandling med årsak=teknisk endring`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.TEKNISK_ENDRING) } returns false

        val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgaver.Companion)
        every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(
            type = FerdigstillOppgaver.TASK_STEP_TYPE,
            payload = "",
        )

        assertThrows<FunksjonellFeil> { beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak) }
    }
}
