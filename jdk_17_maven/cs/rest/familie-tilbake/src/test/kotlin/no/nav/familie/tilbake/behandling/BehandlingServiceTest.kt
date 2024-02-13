package no.nav.familie.tilbake.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.Institusjon
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETILSYN
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegsinfoDto
import no.nav.familie.tilbake.api.dto.ByttEnhetDto
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.api.dto.OpprettRevurderingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.oppgave.OppdaterEnhetOppgaveTask
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.TilgangService
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    private val fom: LocalDate = LocalDate.now().minusMonths(1)
    private val tom: LocalDate = LocalDate.now()

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler() }.returns("Z0000")
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.SYSTEM_TILGANG to Behandlerrolle.SYSTEM)))
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling uten verge`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        behandling.verger.shouldBeEmpty()
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling med verge`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertVerge(behandling, opprettTilbakekrevingRequest)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling uten varsel`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        behandling.varsler.shouldBeEmpty()
        behandling.verger.shouldBeEmpty()
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling fagsak med institusjon`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                finnesInstitusjon = true,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest, true)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        behandling.verger.shouldBeEmpty()
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `opprettBehandling skal legge inn manuellBrevmottaker fra request og autoutføre behandlingssteg BREVMOTTAKER`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                finnesInstitusjon = false,
                finnesManuelleBrevmottakere = true,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val manuelleBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandling.id)
        manuelleBrevmottakere.shouldHaveSize(1)
        manuelleBrevmottakere.first().navn shouldBe "Kari Nordmann"

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.BREVMOTTAKER,
            Behandlingsstegstatus.AUTOUTFØRT,
        )
    }

    @Test
    fun `opprettBehandling oppretter ikke behandling når det finnes åpen tilbakekreving for samme eksternFagsakId`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = shouldThrow<RuntimeException> {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        }
        exception.message shouldBe "Det finnes allerede en åpen behandling for ytelsestype=" +
            opprettTilbakekrevingRequest.ytelsestype +
            " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId}, " +
            "kan ikke opprette en ny."
    }

    @Test
    fun `opprettBehandling skal ikke opprette automatisk behandling når siste tilbakekreving er ikke henlagt`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        }
        exception.message shouldBe
            "Det finnes allerede en avsluttet behandling for ytelsestype=" + opprettTilbakekrevingRequest.ytelsestype +
            " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
            "som ikke er henlagt, kan ikke opprette en ny."
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling når siste tilbakekreving er ikke henlagt og toggelen er på`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandlingRepository = mockk<BehandlingRepository>()
        val fagsakService = mockk<FagsakService>()
        val taskService = mockk<TaskService>(relaxed = true)
        val brevSporingService = mockk<BrevsporingService>(relaxed = true)
        val manuellBrevmottakerRepository = mockk<ManuellBrevmottakerRepository>(relaxed = true)
        val kravgrunnlagRepository = mockk<KravgrunnlagRepository>(relaxed = true)
        val økonomiXmlMottattRepository = mockk<ØkonomiXmlMottattRepository>(relaxed = true)
        val behandlingskontrollService = mockk<BehandlingskontrollService>(relaxed = true)
        val behandlingstilstandService = mockk<BehandlingTilstandService>(relaxed = true)
        val tellerService = mockk<TellerService>(relaxed = true)
        val stegService = mockk<StegService>(relaxed = true)
        val oppgaveTaskService = mockk<OppgaveTaskService>(relaxed = true)
        val historikkTaskService = mockk<HistorikkTaskService>(relaxed = true)
        val tilgangService = mockk<TilgangService>(relaxed = true)
        val integrasjonerClient = mockk<IntegrasjonerClient>(relaxed = true)
        val featureToggleService = mockk<FeatureToggleService>()

        val behandlingServiceMock = BehandlingService(
            behandlingRepository, fagsakService,
            taskService, brevSporingService, manuellBrevmottakerRepository, kravgrunnlagRepository, økonomiXmlMottattRepository,
            behandlingskontrollService, behandlingstilstandService, tellerService, stegService, oppgaveTaskService,
            historikkTaskService, tilgangService, 6, integrasjonerClient, featureToggleService,
        )
        every { featureToggleService.isEnabled(any()) } returns true // default toggelen er av
        every { behandlingRepository.finnÅpenTilbakekrevingsbehandling(any(), any()) } returns null
        every { behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(any()) } returns listOf(Testdata.behandling)
        every { behandlingRepository.insert(any()) } returns Testdata.behandling
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { fagsakService.opprettFagsak(any(), any(), any()) } returns Testdata.fagsak

        val behandling = shouldNotThrowAny { behandlingServiceMock.opprettBehandling(opprettTilbakekrevingRequest) }
        behandling.shouldNotBeNull()
    }

    @Test
    fun `opprettBehandling skal opprette behandling når siste tilbakekreving er henlagt og toggelen er av`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandlingRepository = mockk<BehandlingRepository>()
        val fagsakService = mockk<FagsakService>()
        val taskService = mockk<TaskService>(relaxed = true)
        val brevSporingService = mockk<BrevsporingService>(relaxed = true)
        val manuellBrevmottakerRepository = mockk<ManuellBrevmottakerRepository>(relaxed = true)
        val kravgrunnlagRepository = mockk<KravgrunnlagRepository>(relaxed = true)
        val økonomiXmlMottattRepository = mockk<ØkonomiXmlMottattRepository>(relaxed = true)
        val behandlingskontrollService = mockk<BehandlingskontrollService>(relaxed = true)
        val behandlingstilstandService = mockk<BehandlingTilstandService>(relaxed = true)
        val tellerService = mockk<TellerService>(relaxed = true)
        val stegService = mockk<StegService>(relaxed = true)
        val oppgaveTaskService = mockk<OppgaveTaskService>(relaxed = true)
        val historikkTaskService = mockk<HistorikkTaskService>(relaxed = true)
        val tilgangService = mockk<TilgangService>(relaxed = true)
        val integrasjonerClient = mockk<IntegrasjonerClient>(relaxed = true)
        val featureToggleService = mockk<FeatureToggleService>()

        val behandlingServiceMock = BehandlingService(
            behandlingRepository, fagsakService,
            taskService, brevSporingService, manuellBrevmottakerRepository, kravgrunnlagRepository, økonomiXmlMottattRepository,
            behandlingskontrollService, behandlingstilstandService, tellerService, stegService, oppgaveTaskService,
            historikkTaskService, tilgangService, 6, integrasjonerClient, featureToggleService,
        )
        every { featureToggleService.isEnabled(any()) } returns false // default toggelen er av
        every { behandlingRepository.finnÅpenTilbakekrevingsbehandling(any(), any()) } returns null
        every { behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(any()) } returns listOf(Testdata.behandling.copy(resultater = setOf(Testdata.behandlingsresultat.copy(type = Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT))))
        every { behandlingRepository.insert(any()) } returns Testdata.behandling.copy(resultater = setOf(Testdata.behandlingsresultat.copy(type = Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT)))
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { fagsakService.opprettFagsak(any(), any(), any()) } returns Testdata.fagsak

        val behandling = shouldNotThrowAny { behandlingServiceMock.opprettBehandling(opprettTilbakekrevingRequest) }
        behandling.shouldNotBeNull()
    }

    @Test
    fun `opprettBehandling skal opprette automatisk når det allerede finnes avsluttet behandling for samme fagsak`() {
        val forrigeOpprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(
            finnesVerge = false,
            finnesVarsel = true,
            manueltOpprettet = false,
            tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_MED_VARSEL,
        )
        val forrigeBehandling = behandlingService.opprettBehandling(forrigeOpprettTilbakekrevingRequest)

        val lagretBehandling = behandlingRepository.findByIdOrThrow(forrigeBehandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        // oppretter ny behandling for en annen eksternId
        val nyOpprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(
            finnesVerge = false,
            finnesVarsel = true,
            manueltOpprettet = false,
            tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_MED_VARSEL,
        )
        val behandling = behandlingService.opprettBehandling(nyOpprettTilbakekrevingRequest)
        assertBehandling(behandling, nyOpprettTilbakekrevingRequest)
        assertFagsak(behandling, nyOpprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, nyOpprettTilbakekrevingRequest)
        assertVarselData(behandling, nyOpprettTilbakekrevingRequest)
        behandling.verger.shouldBeEmpty()
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `opprettBehandling skal ikke opprette manuelt når det ikke finnes kravgrunnlag for samme fagsak,ytelsestype,eksternId`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(
            finnesVerge = false,
            finnesVarsel = false,
            manueltOpprettet = true,
            tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
        )

        val exception = shouldThrow<RuntimeException> { behandlingService.opprettBehandling(opprettTilbakekrevingRequest) }
        exception.message shouldBe "Det finnes intet kravgrunnlag for ytelsestype=${opprettTilbakekrevingRequest.ytelsestype}," +
            "eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
            "og eksternId=${opprettTilbakekrevingRequest.eksternId}. " +
            "Tilbakekrevingsbehandling kan ikke opprettes manuelt."
    }

    @Test
    fun `opprettBehandling skal opprette manuelt når det finnes kravgrunnlag for samme fagsak,ytelsestype,eksternId`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(
            finnesVerge = false,
            finnesVarsel = false,
            manueltOpprettet = true,
            tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
        )
        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(
            økonomiXmlMottatt.copy(
                eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                ytelsestype = opprettTilbakekrevingRequest.ytelsestype,
                referanse = opprettTilbakekrevingRequest.eksternId,
            ),
        )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest, true)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        behandling.varsler.shouldBeEmpty()
        behandling.verger.shouldBeEmpty()
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
    }

    @Test
    fun `opprettBehandlingManuellTask skal feile hvis det ikke finnes kravgrunnlag`() {
        shouldThrow<Feil> {
            behandlingService.opprettBehandlingManuellTask(
                OpprettManueltTilbakekrevingRequest(
                    eksternFagsakId = "testverdi",
                    ytelsestype = BARNETRYGD,
                    eksternId = "testverdi",
                ),
            )
        }
    }

    @Test
    fun `opprettBehandlingManuellTask skal opprette OpprettBehandlingManueltTask`() {
        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(
            økonomiXmlMottatt.copy(
                eksternFagsakId = "testverdi",
                ytelsestype = BARNETRYGD,
                referanse = "testverdi",
            ),
        )

        behandlingService.opprettBehandlingManuellTask(
            OpprettManueltTilbakekrevingRequest(
                eksternFagsakId = "testverdi",
                ytelsestype = BARNETRYGD,
                eksternId = "testverdi",
            ),
        )

        val taskene = taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
        taskene.size shouldBe 1
        val task = taskene[0]
        task.type shouldBe OpprettBehandlingManueltTask.TYPE
        task.metadata["eksternFagsakId"] shouldBe "testverdi"
        task.metadata["ytelsestype"] shouldBe BARNETRYGD.name
        task.metadata["eksternId"] shouldBe "testverdi"
        task.metadata["ansvarligSaksbehandler"] shouldBe "Z0000"
    }

    @Test
    fun `opprettRevurdering skal opprette revurdering for gitt avsluttet tilbakekrevingsbehandling`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        var revurdering = behandlingService.opprettRevurdering(lagOpprettRevurderingDto(behandling.id))
        revurdering = behandlingRepository.findByIdOrThrow(revurdering.id)
        revurdering.type shouldBe Behandlingstype.REVURDERING_TILBAKEKREVING
        revurdering.sisteÅrsak?.type shouldBe Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR
        revurdering.status shouldBe Behandlingsstatus.UTREDES
        revurdering.behandlendeEnhet shouldBe behandling.behandlendeEnhet
        revurdering.behandlendeEnhetsNavn shouldBe behandling.behandlendeEnhetsNavn
        behandling.manueltOpprettet.shouldBeFalse()

        val aktivFagsystemsbehandling = revurdering.aktivFagsystemsbehandling
        aktivFagsystemsbehandling.tilbakekrevingsvalg shouldBe behandling.aktivFagsystemsbehandling.tilbakekrevingsvalg
        aktivFagsystemsbehandling.revurderingsvedtaksdato shouldBe behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato
        aktivFagsystemsbehandling.eksternId shouldBe behandling.aktivFagsystemsbehandling.eksternId
        aktivFagsystemsbehandling.årsak shouldBe behandling.aktivFagsystemsbehandling.årsak
        aktivFagsystemsbehandling.resultat shouldBe behandling.aktivFagsystemsbehandling.resultat
        assertHistorikkTask(revurdering.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(
            revurdering.id,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Aktør.VEDTAKSLØSNING,
            "Venter på kravgrunnlag fra økonomi",
        )
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            HentKravgrunnlagTask.TYPE == it.type &&
                revurdering.id.toString() == it.payload
        }
        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(revurdering.id)
        behandlingsstegstilstand.shouldNotBeNull()
        behandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
        behandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.VENTER
    }

    @Test
    fun `opprettRevurdering skal ikke opprette revurdering for tilbakekreving som er avsluttet uten kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> {
            behandlingService.opprettRevurdering(lagOpprettRevurderingDto(behandling.id))
        }
        exception.message shouldBe "Revurdering kan ikke opprettes for behandling ${behandling.id}. " +
            "Enten behandlingen er ikke avsluttet med kravgrunnlag eller " +
            "det finnes allerede en åpen revurdering"
    }

    @Test
    fun `hentBehandling skal hente behandling som opprettet uten varsel`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, behandling)
        behandlingDto.kanHenleggeBehandling.shouldBeFalse()
        behandlingDto.harVerge.shouldBeTrue()
        behandlingDto.harVerge.shouldBeTrue()
        behandlingDto.erBehandlingPåVent.shouldBeTrue()
        behandlingDto.kanEndres.shouldBeTrue()
        assertBehandlingsstegsinfo(
            behandlingDto = behandlingDto,
            behandling = behandling,
            behandlingssteg = Behandlingssteg.GRUNNLAG,
            behandlingsstegstatus = Behandlingsstegstatus.VENTER,
            venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
    }

    @Test
    fun `hentBehandling skal hente behandling som ikke kan henlegges med verge`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, behandling)
        behandlingDto.kanHenleggeBehandling.shouldBeFalse()
        behandlingDto.harVerge.shouldBeTrue()
        behandlingDto.erBehandlingPåVent.shouldBeTrue()
        behandlingDto.kanEndres.shouldBeTrue()
        assertBehandlingsstegsinfo(
            behandlingDto = behandlingDto,
            behandling = behandling,
            behandlingssteg = Behandlingssteg.VARSEL,
            behandlingsstegstatus = Behandlingsstegstatus.VENTER,
            venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `hentBehandling skal hente behandling som kan henlegges uten verge`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val sporbar = behandling.sporbar.copy(opprettetTid = LocalDate.now().minusDays(10).atStartOfDay())
        val oppdatertBehandling = lagretBehandling.copy(sporbar = sporbar)
        behandlingRepository.update(oppdatertBehandling)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, oppdatertBehandling)
        behandlingDto.kanHenleggeBehandling.shouldBeTrue()
        behandlingDto.harVerge.shouldBeFalse()
        behandlingDto.erBehandlingPåVent.shouldBeTrue()
        behandlingDto.kanEndres.shouldBeTrue()
        assertBehandlingsstegsinfo(
            behandlingDto = behandlingDto,
            behandling = behandling,
            behandlingssteg = Behandlingssteg.VARSEL,
            behandlingsstegstatus = Behandlingsstegstatus.VENTER,
            venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )
    }

    @Test
    fun `hentBehandling skal hente behandling når behandling er avsluttet`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeFalse()
        behandlingDto.kanHenleggeBehandling.shouldBeFalse()
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av veileder`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.VEILEDER)))

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeFalse()
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av forvalter`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER)))

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeFalse()
    }

    @Test
    fun `hentBehandling skal endre behandling av saksbehandler`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(
                InnloggetBrukertilgang(
                    mapOf(
                        Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER,
                        Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.SAKSBEHANDLER,
                    ),
                ),
            )

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeTrue()
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av saksbehandler når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.SAKSBEHANDLER)))

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeFalse()
    }

    @Test
    fun `hentBehandling skal endre behandling av beslutter når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            lagretBehandling.copy(
                status = Behandlingsstatus.FATTER_VEDTAK,
                ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            ),
        )

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeTrue()
    }

    @Test
    fun `hentBehandling skal ikke endre behandling med fattevedtak steg og beslutter er samme som saksbehandler`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            lagretBehandling.copy(
                status = Behandlingsstatus.FATTER_VEDTAK,
                ansvarligSaksbehandler = "Z0000",
            ),
        )

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.kanEndres.shouldBeFalse()
    }

    @Test
    fun `hentBehandling kan ikke opprette revurdering når tilbakekreving ikke har kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        behandlingDto.kanRevurderingOpprettes.shouldBeFalse()
    }

    @Test
    fun `hentBehandling kan opprette revurdering når tilbakekreving er avsluttet med kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        behandlingDto.kanRevurderingOpprettes.shouldBeTrue()
    }

    @Test
    fun `hentBehandling kan ikke opprette revurdering når tilbakekreving har en åpen revurdering`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        behandlingRepository.insert(Testdata.revurdering)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        behandlingDto.kanRevurderingOpprettes.shouldBeFalse()
    }

    @Test
    fun `hentBehandling kan opprette revurdering når tilbakekreving har en avsluttet revurdering`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        var revurdering = behandlingRepository.insert(Testdata.revurdering)
        revurdering = behandlingRepository.findByIdOrThrow(revurdering.id)
        behandlingRepository.update(revurdering.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        behandlingDto.kanRevurderingOpprettes.shouldBeTrue()
    }

    @Test
    fun `settBehandlingPåVent skal ikke sett behandling på vent hvis fristdato er mindre enn i dag`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = shouldThrow<RuntimeException> {
            behandlingService.settBehandlingPåVent(
                behandling.id,
                BehandlingPåVentDto(
                    venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                    tidsfrist = LocalDate.now().minusDays(4),
                ),
            )
        }
        exception.message shouldBe "Fristen må være større enn dagens dato for behandling ${behandling.id}"
    }

    @Test
    fun `settBehandlingPåVent skal ikke sett behandling på vent hvis fristdato er i dag`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = shouldThrow<RuntimeException> {
            behandlingService.settBehandlingPåVent(
                behandling.id,
                BehandlingPåVentDto(
                    venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                    tidsfrist = LocalDate.now(),
                ),
            )
        }
        exception.message shouldBe "Fristen må være større enn dagens dato for behandling ${behandling.id}"
    }

    @Test
    fun `settBehandlingPåVent skal sette behandling på vent hvis fristdato er større enn i dag`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingPåVentDto = BehandlingPåVentDto(
            venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
            tidsfrist = LocalDate.now().plusDays(1),
        )

        behandlingService.settBehandlingPåVent(behandling.id, behandlingPåVentDto)

        behandlingskontrollService.erBehandlingPåVent(behandling.id).shouldBeTrue()
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Aktør.SAKSBEHANDLER,
            Venteårsak.ENDRE_TILKJENT_YTELSE.beskrivelse,
        )
        assertOppgaveTask(
            behandling.id,
            OppdaterOppgaveTask.TYPE,
            "Frist er oppdatert av saksbehandler Z0000",
            behandlingPåVentDto.tidsfrist,
        )
    }

    @Test
    fun `taBehandlingAvvent skal ikke gjenoppta når behandling er avsluttet`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> { behandlingService.taBehandlingAvvent(lagretBehandling.id) }
        exception.message shouldBe "Behandling med id=${lagretBehandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `taBehandlingAvvent skal ikke gjenoppta når behandling er ikke på vent`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling)

        val exception = shouldThrow<RuntimeException> { behandlingService.taBehandlingAvvent(behandling.id) }
        exception.message shouldBe "Behandling ${behandling.id} er ikke på vent, kan ike gjenoppta"
    }

    @Test
    fun `taBehandlingAvvent skal gjenoppta når behandling er på vent`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(behandlingId = behandling.id))

        behandlingService.settBehandlingPåVent(
            behandlingId = behandling.id,
            behandlingPåVentDto = BehandlingPåVentDto(
                Venteårsak.AVVENTER_DOKUMENTASJON,
                LocalDate.now().plusDays(2),
            ),
        )

        behandlingskontrollService.erBehandlingPåVent(behandling.id).shouldBeTrue()

        behandlingService.taBehandlingAvvent(behandling.id)

        behandlingskontrollService.erBehandlingPåVent(behandling.id).shouldBeFalse()
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT, Aktør.SAKSBEHANDLER)
        assertOppgaveTask(
            behandling.id,
            OppdaterOppgaveTask.TYPE,
            "Behandling er tatt av vent",
            LocalDate.now(),
        )
    }

    @Test
    fun `taBehandlingAvvent skal gjenoppta behandling og hoppe til FAKTA steg når behandling venter på bruker med grunnlag`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER &&
                it.venteårsak == Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        }

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(behandlingId = behandling.id))

        behandlingService.taBehandlingAvvent(behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.FAKTA &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
        }
        behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.GRUNNLAG }.shouldBeFalse()

        behandlingskontrollService.erBehandlingPåVent(behandling.id).shouldBeFalse()
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT, Aktør.SAKSBEHANDLER)
        assertOppgaveTask(
            behandling.id,
            OppdaterOppgaveTask.TYPE,
            "Behandling er tatt av vent",
            LocalDate.now(),
        )
    }

    @Test
    fun `taBehandlingAvvent skal gjenoppta behandling og venter på GRUNNLAG steg når behandling venter på bruker`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = true,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER &&
                it.venteårsak == Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        }

        behandlingService.taBehandlingAvvent(behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.GRUNNLAG &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER
        }

        behandlingskontrollService.erBehandlingPåVent(behandling.id).shouldBeTrue()
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT, Aktør.SAKSBEHANDLER)
        assertOppgaveTask(
            behandling.id,
            OppdaterOppgaveTask.TYPE,
            "Behandling er tatt av vent",
            LocalDate.now(),
        )
        assertOppgaveTask(
            behandling.id,
            OppdaterOppgaveTask.TYPE,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
            LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker),
        )
    }

    @Test
    fun `henleggBehandling skal henlegge behandling og sende henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = true,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        // oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(
            behandling.copy(
                sporbar = Sporbar(
                    opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                    opprettetTid = LocalDateTime.now().minusDays(10),
                ),
            ),
        )
        // sender varselsbrev
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandling.id,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
                brevtype = Brevtype.VARSEL,
            ),
        )
        behandlingService.taBehandlingAvvent(behandlingId = behandling.id)

        behandlingService.henleggBehandling(
            behandling.id,
            HenleggelsesbrevFritekstDto(
                Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
                "testverdi",
            ),
        )

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET
        behandling.avsluttetDato shouldBe LocalDate.now()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 2
        behandlingsstegstilstand.filter { it.behandlingssteg == Behandlingssteg.VARSEL }.size shouldBe 1
        behandlingsstegstilstand.first { it.behandlingssteg == Behandlingssteg.VARSEL }.behandlingsstegsstatus shouldBe Behandlingsstegstatus.UTFØRT
        behandlingsstegstilstand.filter { it.behandlingssteg == Behandlingssteg.GRUNNLAG }.size shouldBe 1
        behandlingsstegstilstand.first { it.behandlingssteg == Behandlingssteg.GRUNNLAG }.behandlingsstegsstatus shouldBe Behandlingsstegstatus.AVBRUTT

        val behandlingssresultat = behandling.sisteResultat
        behandlingssresultat.shouldNotBeNull()
        behandlingssresultat.type shouldBe Behandlingsresultatstype.HENLAGT_FEILOPPRETTET

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).any { it.type == SendHenleggelsesbrevTask.TYPE }.shouldBeTrue()
        assertHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            Aktør.SAKSBEHANDLER,
            "testverdi",
        )
        assertOppgaveTask(behandling.id, FerdigstillOppgaveTask.TYPE)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling uten henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        // oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(
            behandling.copy(
                sporbar = Sporbar(
                    opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                    opprettetTid = LocalDateTime.now().minusDays(10),
                ),
            ),
        )

        behandlingService.henleggBehandling(
            behandling.id,
            HenleggelsesbrevFritekstDto(
                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                "testverdi",
            ),
        )

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET
        behandling.avsluttetDato shouldBe LocalDate.now()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        behandlingsstegstilstand[0].behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
        behandlingsstegstilstand[0].behandlingsstegsstatus shouldBe Behandlingsstegstatus.AVBRUTT

        val behandlingssresultat = behandling.sisteResultat
        behandlingssresultat.shouldNotBeNull()
        behandlingssresultat.type shouldBe Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).find { task -> task.type == SendHenleggelsesbrevTask.TYPE }.shouldBeNull()
        assertHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            Aktør.VEDTAKSLØSNING,
            "testverdi",
        )
        assertOppgaveTask(behandling.id, FerdigstillOppgaveTask.TYPE)
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som opprettet nå`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = shouldThrow<RuntimeException> {
            behandlingService
                .henleggBehandling(
                    behandling.id,
                    HenleggelsesbrevFritekstDto(
                        Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        "testverdi",
                    ),
                )
        }
        exception.message shouldBe "Behandling med behandlingId=${behandling.id} kan ikke henlegges."
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som har aktivt kravgrunnlag`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val kravgrunnlag = Testdata.kravgrunnlag431
        kravgrunnlagRepository.insert(kravgrunnlag.copy(behandlingId = behandling.id))

        val exception = shouldThrow<RuntimeException> {
            behandlingService
                .henleggBehandling(
                    behandling.id,
                    HenleggelsesbrevFritekstDto(
                        Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        "testverdi",
                    ),
                )
        }
        exception.message shouldBe "Behandling med behandlingId=${behandling.id} kan ikke henlegges."
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som er allerede avsluttet`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> {
            behandlingService
                .henleggBehandling(
                    behandling.id,
                    HenleggelsesbrevFritekstDto(
                        Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                        "testverdi",
                    ),
                )
        }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `byttBehandlendeEnhet skal bytte og oppdatere oppgave`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        behandlingService.byttBehandlendeEnhet(
            behandling.id,
            ByttEnhetDto(
                "4806",
                "bytter i unittest" +
                    "\n\nmed linjeskift" +
                    "\n\nto til og med",
            ),
        )

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.behandlendeEnhet shouldBe "4806"
        behandling.behandlendeEnhetsNavn shouldBe "Mock NAV Drammen"

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).any {
            it.type == OppdaterEnhetOppgaveTask.TYPE &&
                "Endret tildelt enhet: 4806" == it.metadata["beskrivelse"] &&
                "4806" == it.metadata["enhetId"]
        }.shouldBeTrue()
        assertHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
            Aktør.SAKSBEHANDLER,
            "bytter i unittest  med linjeskift  to til og med",
        )
    }

    @Test
    fun `byttBehandlendeEnhet skal ikke kunne bytte på behandling med fagsystem EF`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
                .copy(
                    fagsystem = Fagsystem.EF,
                    ytelsestype = BARNETILSYN,
                )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        val exception = shouldThrow<RuntimeException> {
            behandlingService.byttBehandlendeEnhet(behandling.id, ByttEnhetDto("4806", "bytter i unittest"))
        }
        exception.message shouldBe "Ikke implementert for fagsystem EF"
    }

    @Test
    fun `byttBehandlendeEnhet skal ikke kunne bytte på avsluttet behandling`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesVerge = false,
                finnesVarsel = false,
                manueltOpprettet = false,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> {
            behandlingService.byttBehandlendeEnhet(behandling.id, ByttEnhetDto("4806", "bytter i unittest"))
        }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `Behandling på fagsak av type institusjon skal ikke støtte manuelle brevmottakere`() {
        val opprettTilbakekrevingRequest =
            lagOpprettTilbakekrevingRequest(
                finnesInstitusjon = true,
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        behandlingDto.støtterManuelleBrevmottakere shouldBe false
    }

    private fun assertFellesBehandlingRespons(
        behandlingDto: BehandlingDto,
        behandling: Behandling,
    ) {
        behandlingDto.eksternBrukId shouldBe behandling.eksternBrukId
        behandlingDto.erBehandlingHenlagt.shouldBeFalse()
        behandlingDto.type shouldBe Behandlingstype.TILBAKEKREVING
        behandlingDto.status shouldBe Behandlingsstatus.UTREDES
        behandlingDto.opprettetDato shouldBe behandling.opprettetDato
        behandlingDto.avsluttetDato.shouldBeNull()
        behandlingDto.vedtaksdato.shouldBeNull()
        behandlingDto.enhetskode shouldBe "8020"
        behandlingDto.enhetsnavn shouldBe "Oslo"
        behandlingDto.resultatstype.shouldBeNull()
        behandlingDto.ansvarligSaksbehandler shouldBe "bb1234"
        behandlingDto.ansvarligBeslutter.shouldBeNull()
        behandlingDto.kanRevurderingOpprettes.shouldBeFalse()
    }

    private fun assertBehandlingsstegsinfo(
        behandlingDto: BehandlingDto,
        behandling: Behandling,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak,
    ) {
        val behandlingsstegsinfo: List<BehandlingsstegsinfoDto> = behandlingDto.behandlingsstegsinfo
        behandlingsstegsinfo.size shouldBe 1
        behandlingsstegsinfo[0].behandlingssteg shouldBe behandlingssteg
        behandlingsstegsinfo[0].behandlingsstegstatus shouldBe behandlingsstegstatus
        behandlingsstegsinfo[0].venteårsak shouldBe venteårsak
        behandlingsstegsinfo[0].tidsfrist shouldBe behandling.opprettetDato.plusWeeks(venteårsak.defaultVenteTidIUker)
    }

    private fun assertBehandlingsstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
    ) {
        behandlingsstegstilstand.any {
            it.behandlingssteg == behandlingssteg &&
                it.behandlingsstegsstatus == behandlingsstegstatus
            it.venteårsak == venteårsak
        }.shouldBeTrue()
    }

    private fun assertFagsak(
        behandling: Behandling,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
        finnesInstitusjon: Boolean = false,
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        fagsak.eksternFagsakId shouldBe opprettTilbakekrevingRequest.eksternFagsakId
        fagsak.ytelsestype.name shouldBe opprettTilbakekrevingRequest.ytelsestype.name
        fagsak.fagsystem shouldBe opprettTilbakekrevingRequest.fagsystem
        fagsak.bruker.språkkode shouldBe opprettTilbakekrevingRequest.språkkode
        fagsak.bruker.ident shouldBe opprettTilbakekrevingRequest.personIdent
        if (finnesInstitusjon) {
            fagsak.institusjon shouldNotBe null
            fagsak.institusjon!!.organisasjonsnummer shouldBe opprettTilbakekrevingRequest.institusjon!!.organisasjonsnummer
        } else {
            fagsak.institusjon shouldBe null
        }
    }

    private fun assertBehandling(
        behandling: Behandling,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
        manueltOpprettet: Boolean? = false,
    ) {
        behandling.type.name shouldBe Behandlingstype.TILBAKEKREVING.name
        behandling.status.name shouldBe Behandlingsstatus.OPPRETTET.name
        behandling.manueltOpprettet shouldBe manueltOpprettet
        behandling.behandlendeEnhet shouldBe opprettTilbakekrevingRequest.enhetId
        behandling.behandlendeEnhetsNavn shouldBe opprettTilbakekrevingRequest.enhetsnavn
        behandling.saksbehandlingstype.name shouldBe Saksbehandlingstype.ORDINÆR.name
        behandling.opprettetDato shouldBe LocalDate.now()
    }

    private fun assertFagsystemsbehandling(
        behandling: Behandling,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ) {
        val fagsystemsbehandlinger = behandling.fagsystemsbehandling
        fagsystemsbehandlinger.size shouldBe 1
        val fagsystemsbehandling = fagsystemsbehandlinger.toList().first()
        fagsystemsbehandling.aktiv shouldBe true
        fagsystemsbehandling.eksternId shouldBe opprettTilbakekrevingRequest.eksternId
        fagsystemsbehandling.tilbakekrevingsvalg shouldBe opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg
        fagsystemsbehandling.revurderingsvedtaksdato shouldBe opprettTilbakekrevingRequest.revurderingsvedtaksdato
        fagsystemsbehandling.resultat shouldBe "testresultat"
        fagsystemsbehandling.årsak shouldBe "testverdi"
        fagsystemsbehandling.konsekvenser.shouldBeEmpty()
    }

    private fun assertVarselData(
        behandling: Behandling,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ) {
        val varsler = behandling.varsler
        varsler.size shouldBe 1
        val varsel = varsler.toList().first()
        opprettTilbakekrevingRequest.varsel?.let {
            varsel.varseltekst shouldBe it.varseltekst
            varsel.varselbeløp.toBigDecimal() shouldBe it.sumFeilutbetaling
            varsel.perioder.size shouldBe it.perioder.size
            varsel.perioder.toList().first().fom shouldBe it.perioder.first().fom
            varsel.perioder.toList().first().tom shouldBe it.perioder.first().tom
        }
    }

    private fun assertVerge(
        behandling: Behandling,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ) {
        behandling.verger.shouldNotBeEmpty()
        behandling.verger.size shouldBe 1
        val verge = behandling.verger.toList().first()
        verge.type.navn shouldBe opprettTilbakekrevingRequest.verge?.vergetype?.navn
        verge.navn shouldBe opprettTilbakekrevingRequest.verge?.navn
        verge.orgNr shouldBe opprettTilbakekrevingRequest.verge?.organisasjonsnummer
        verge.ident shouldBe opprettTilbakekrevingRequest.verge?.personIdent
    }

    private fun lagOpprettTilbakekrevingRequest(
        tilbakekrevingsvalg: Tilbakekrevingsvalg,
        finnesVerge: Boolean = false,
        finnesVarsel: Boolean = false,
        manueltOpprettet: Boolean = false,
        finnesInstitusjon: Boolean = false,
        finnesManuelleBrevmottakere: Boolean = false,
    ): OpprettTilbakekrevingRequest {
        val varsel = if (finnesVarsel) {
            Varsel(
                varseltekst = "testverdi",
                sumFeilutbetaling = BigDecimal.valueOf(1500L),
                perioder = listOf(Periode(fom, tom)),
            )
        } else {
            null
        }
        val verge = if (finnesVerge) {
            Verge(
                vergetype = Vergetype.VERGE_FOR_BARN,
                navn = "Andy",
                personIdent = "321321321",
            )
        } else {
            null
        }

        val faktainfo = Faktainfo(
            revurderingsårsak = "testverdi",
            revurderingsresultat = "testresultat",
            tilbakekrevingsvalg = tilbakekrevingsvalg,
        )
        val institusjon =
            if (finnesInstitusjon) Institusjon(organisasjonsnummer = "987654321") else null

        val manuelleBrevmottakere = if (finnesManuelleBrevmottakere) {
            setOf(
                Brevmottaker(
                    type = MottakerType.DØDSBO,
                    navn = "Kari Nordmann",
                    manuellAdresseInfo = ManuellAdresseInfo(
                        "testadresse",
                        postnummer = "0000",
                        poststed = "OSLO",
                        landkode = "NO",
                    ),
                ),
            )
        } else {
            emptySet()
        }

        return OpprettTilbakekrevingRequest(
            ytelsestype = BARNETRYGD,
            fagsystem = Fagsystem.BA,
            eksternFagsakId = "1234567",
            personIdent = "321321322",
            eksternId = UUID.randomUUID().toString(),
            manueltOpprettet = manueltOpprettet,
            språkkode = Språkkode.NN,
            enhetId = "8020",
            enhetsnavn = "Oslo",
            varsel = varsel,
            revurderingsvedtaksdato = fom,
            verge = verge,
            faktainfo = faktainfo,
            saksbehandlerIdent = "Z0000",
            institusjon = institusjon,
            manuelleBrevmottakere = manuelleBrevmottakere,
        )
    }

    private fun lagOpprettRevurderingDto(originalBehandlingId: UUID): OpprettRevurderingDto {
        return OpprettRevurderingDto(BARNETRYGD, originalBehandlingId, Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR)
    }

    private fun assertAnsvarligSaksbehandler(behandling: Behandling) {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        lagretBehandling.ansvarligSaksbehandler shouldBe "Z0000"
        lagretBehandling.ansvarligBeslutter.shouldBeNull()
    }

    private fun assertHistorikkTask(
        behandlingId: UUID,
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        tekst: String? = null,
    ) {
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).any {
            LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                aktør.name == it.metadata["aktør"] &&
                behandlingId.toString() == it.payload &&
                tekst == it.metadata["beskrivelse"]
        }.shouldBeTrue()
    }

    private fun assertFinnKravgrunnlagTask(behandlingId: UUID) {
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).any {
            FinnKravgrunnlagTask.TYPE == it.type && behandlingId.toString() == it.payload
        }.shouldBeTrue()
    }

    private fun assertOppgaveTask(
        behandlingId: UUID,
        taskType: String,
        beskrivelse: String? = null,
        frist: LocalDate? = null,
    ) {
        taskService.findAll().any {
            it.type == taskType &&
                behandlingId.toString() == it.payload &&
                Oppgavetype.BehandleSak.value == it.metadata["oppgavetype"]
            beskrivelse == it.metadata["beskrivelse"] &&
                frist == it.metadata["frist"]?.let { dato -> LocalDate.parse(dato as CharSequence) }
        }.shouldBeTrue()
    }
}
