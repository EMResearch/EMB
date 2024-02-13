package no.nav.familie.tilbake.behandling.steg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVergeDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.GodTroDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.api.dto.VurdertTotrinnDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.VergeService
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.totrinn.TotrinnsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    @Autowired
    private lateinit var vergeService: VergeService

    @Autowired
    private lateinit var stegService: StegService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    private val FOM = YearMonth.now().minusMonths(1).atDay(1)
    private val TOM = YearMonth.now().atEndOfMonth()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler() }.returns("Z0000")
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `håndterSteg skal utføre grunnlagssteg og fortsette til Fakta steg når behandling ikke har verge og har fått grunnlag`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(verger = emptySet()))

        lagBehandlingsstegstilstand(
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.håndterSteg(behandlingId)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `håndterSteg skal utføre grunnlagssteg,autoutføre verge steg og fortsette til Fakta steg når behandling har verge`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.håndterSteg(behandlingId)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `håndterSteg skal ikke utføre faktafeilutbetaling når behandling er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        val exception = shouldThrow<RuntimeException> { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) }
        exception.message shouldBe "Behandling med id=$behandlingId er allerede ferdig behandlet"
    }

    @Test
    fun `håndterStegAutomatisk skal ikke utføre automatisk behandling når den følger EØS-regelverket`() {
        behandlingRepository.findByIdOrThrow(behandlingId)
            .copy(regelverk = Regelverk.EØS)
            .also { behandlingRepository.update(it) }

        val exception = shouldThrow<RuntimeException> { stegService.håndterStegAutomatisk(behandlingId) }
        exception.message shouldBe "Behandling med id=$behandlingId behandles etter EØS-regelverket, og skal dermed ikke behandles automatisk."
    }

    @Test
    fun `håndterSteg skal ikke utføre faktafeilutbetaling når behandling er på vent`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.FAKTA,
            Behandlingsstegstatus.VENTER,
            Venteårsak.AVVENTER_DOKUMENTASJON,
        )

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        val exception = shouldThrow<RuntimeException> { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) }
        exception.message shouldBe "Behandling med id=$behandlingId er på vent, kan ikke behandle steg FAKTA"
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetalingssteg for behandling`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()
        stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstander.size shouldBe 3
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        aktivtBehandlingssteg?.behandlingssteg shouldBe Behandlingssteg.VILKÅRSVURDERING
        aktivtBehandlingssteg?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)

        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetaling og fortsette til vilkårsvurdering når behandling er på foreslåvedtak`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstander.size shouldBe 4
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        aktivtBehandlingssteg?.behandlingssteg shouldBe Behandlingssteg.VILKÅRSVURDERING
        aktivtBehandlingssteg?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)

        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetaling og fortsette til foreldelse når foreldelse ikke er autoutført`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                kravgrunnlag431.copy(
                    perioder =
                    setOf(
                        grunnlagsperiode.copy(
                            periode = Månedsperiode(
                                fom = LocalDate.of(2010, 1, 1),
                                tom = LocalDate.of(2010, 1, 31),
                            ),
                        ),
                    ),
                )
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val faktaFeilutbetaltePerioderDto = FaktaFeilutbetalingsperiodeDto(
            periode = Datoperiode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 1, 31),
            ),
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        val behandlingsstegFaktaDto = BehandlingsstegFaktaDto(
            feilutbetaltePerioder = listOf(faktaFeilutbetaltePerioderDto),
            begrunnelse = "testverdi",
        )

        stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstander.size shouldBe 4
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        aktivtBehandlingssteg?.behandlingssteg shouldBe Behandlingssteg.FORELDELSE
        aktivtBehandlingssteg?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)
        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til foreslå vedtak når alle perioder er foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                kravgrunnlag431.copy(
                    perioder =
                    setOf(
                        grunnlagsperiode.copy(
                            periode = Månedsperiode(
                                fom = LocalDate.of(2010, 1, 1),
                                tom = LocalDate.of(2010, 1, 31),
                            ),
                        ),
                    ),
                )
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegForeldelseDto =
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            LocalDate.of(2010, 1, 1),
                            LocalDate.of(2010, 1, 31),
                        ),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.FORELDET,
                    ),
                ),
            )
        stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstander.size shouldBe 4
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        aktivtBehandlingssteg?.behandlingssteg shouldBe Behandlingssteg.FORESLÅ_VEDTAK
        aktivtBehandlingssteg?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.AUTOUTFØRT,
        )
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertForeldelsesdata(behandlingsstegForeldelseDto.foreldetPerioder[0])

        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til vilkårsvurdering når minst en periode ikke er foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        val førstePeriode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 1, 31),
                ),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                ),
            )
        val andrePeriode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(
                    fom = LocalDate.of(2018, 2, 1),
                    tom = LocalDate.of(2018, 2, 28),
                ),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                ),
            )

        val kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode))
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegForeldelseDto =
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        førstePeriode.periode.toDatoperiode(),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.FORELDET,
                    ),
                    ForeldelsesperiodeDto(
                        andrePeriode.periode.toDatoperiode(),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.IKKE_FORELDET,
                    ),
                ),
            )
        stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstander.size shouldBe 3
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        aktivtBehandlingssteg?.behandlingssteg shouldBe Behandlingssteg.VILKÅRSVURDERING
        aktivtBehandlingssteg?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til foreslå vedtak når alle perioder endret til foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                kravgrunnlag431.copy(
                    perioder = setOf(
                        grunnlagsperiode.copy(
                            periode = Månedsperiode(
                                LocalDate.of(2010, 1, 1),
                                LocalDate.of(2010, 1, 31),
                            ),
                        ),
                    ),
                )
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        // foreldelsesteg vurderte som IKKE_FORELDET med første omgang
        var behandlingsstegForeldelseDto =
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            LocalDate.of(2010, 1, 1),
                            LocalDate.of(2010, 1, 31),
                        ),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.IKKE_FORELDET,
                    ),
                ),
            )
        stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto)
        var behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        // behandle vilkårsvurderingssteg
        val behandlingsstegVilkårsvurderingDto = lagBehandlingsstegVilkårsvurderingDto(
            Datoperiode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 1, 31),
            ),
        )
        stegService.håndterSteg(behandlingId, behandlingsstegVilkårsvurderingDto)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.UTFØRT,
        )
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        // behandler foreldelse steg på nytt og endrer periode til foreldet
        behandlingsstegForeldelseDto =
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            LocalDate.of(2010, 1, 1),
                            LocalDate.of(2010, 1, 31),
                        ),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.FORELDET,
                    ),
                ),
            )
        stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.AUTOUTFØRT,
        )
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        // deaktiverte tildligere behandlet vilkårsvurdering når alle perioder er foreldet
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId).shouldBeNull()

        // historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre foreslå vedtak og forsette til fatte vedtak`() {
        // behandle fakta steg
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()
        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        // behandle vilkårsvurderingssteg
        stegService.håndterSteg(behandlingId, lagBehandlingsstegVilkårsvurderingDto(Datoperiode(FOM, TOM)))

        val fritekstavsnitt =
            FritekstavsnittDto(
                perioderMedTekst = listOf(
                    PeriodeMedTekstDto(
                        periode = Datoperiode(FOM, TOM),
                        faktaAvsnitt = "fakta tekst",
                    ),
                ),
            )
        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt))
        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.UTFØRT,
        )
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.FATTER_VEDTAK)
        assertFaktadata(behandlingsstegFaktaDto)

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TIL_BESLUTTER, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreslå vedtak på nytt når beslutter underkjente steg og forsette til fatte vedtak`() {
        // behandle fakta steg
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        // behandle vilkårsvurderingssteg
        stegService.håndterSteg(behandlingId, lagBehandlingsstegVilkårsvurderingDto(Datoperiode(FOM, TOM)))

        val fritekstavsnitt = FritekstavsnittDto(
            perioderMedTekst = listOf(
                PeriodeMedTekstDto(
                    periode = Datoperiode(FOM, TOM),
                    faktaAvsnitt = "fakta tekst",
                ),
            ),
        )
        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt = fritekstavsnitt))

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = false))

        assertOppgave(FerdigstillOppgaveTask.TYPE, 2)
        assertOppgave(LagOppgaveTask.TYPE, 2)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FATTE_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )

        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt = fritekstavsnitt))

        assertOppgave(FerdigstillOppgaveTask.TYPE, 3)
        assertOppgave(LagOppgaveTask.TYPE, 3)

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER,
            Aktør.BESLUTTER,
        )
    }

    @Test
    fun `håndterSteg skal utføre fatte vedtak og forsette til iverksette vedtak når beslutter godkjenner alt`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = true))

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.IVERKSETT_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.UTFØRT)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandling.ansvarligBeslutter shouldBe "Z0000"
        behandling.status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        totrinnsvurderinger.shouldNotBeEmpty()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FAKTA && it.godkjent }.shouldBeTrue()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORELDELSE }.shouldBeFalse()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING && it.godkjent }.shouldBeTrue()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORESLÅ_VEDTAK && it.godkjent }.shouldBeTrue()

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET, Aktør.BESLUTTER)

        val behandlingsresultat = behandling.sisteResultat
        behandlingsresultat.shouldNotBeNull()
        behandlingsresultat.type shouldBe Behandlingsresultatstype.INGEN_TILBAKEBETALING
        val behandlingsvedtak = behandlingsresultat.behandlingsvedtak
        behandlingsvedtak.shouldNotBeNull()
        behandlingsvedtak.iverksettingsstatus shouldBe Iverksettingsstatus.UNDER_IVERKSETTING
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
            .any { it.type == SendØkonomiTilbakekrevingsvedtakTask.TYPE }.shouldBeTrue()
    }

    @Test
    fun `håndterSteg skal tilbakeføre fatte vedtak og flytte til foreslå vedtak når beslutter underkjente steg`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = false))

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FATTE_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandling.ansvarligBeslutter shouldBe null
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        totrinnsvurderinger.shouldNotBeEmpty()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FAKTA && !it.godkjent }.shouldBeTrue()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORELDELSE }.shouldBeFalse()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING && !it.godkjent }
            .shouldBeTrue()
        totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORESLÅ_VEDTAK && !it.godkjent }.shouldBeTrue()

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)
    }

    @Test
    fun `håndterSteg skal ikke utføre fakta steg når behandling er på fatte vedtak steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId).shouldBeEmpty()
    }

    @Test
    fun `håndterSteg skal ikke utføre fatte vedtak steg når beslutter er samme som saksbehandler`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                status = Behandlingsstatus.FATTER_VEDTAK,
                ansvarligSaksbehandler = "Z0000",
            ),
        )

        val exception = shouldThrow<RuntimeException> {
            stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = true))
        }

        exception.message shouldBe "ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler"
    }

    @Test
    fun `håndterSteg skal opprette og utføre verge steg når behandling er på foreslå vedtak`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)

        vergeService.opprettVergeSteg(behandlingId)

        var behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)

        val vergeData = BehandlingsstegVergeDto(
            verge = VergeDto(
                ident = "32132132111",
                type = Vergetype.VERGE_FOR_BARN,
                navn = "testverdi",
                begrunnelse = "testverdi",
            ),
        )
        stegService.håndterSteg(behandlingId, vergeData)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(
            behandlingsstegstilstander,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling og fortsette til grunnlag når behandling er i varselssteg uten grunnlag`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
        aktivtBehandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.VENTER
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
        aktivtBehandlingsstegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        aktivtBehandlingsstegstilstand.tidsfrist shouldBe LocalDate.now()
            .plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling og fortsette til fakta når behandling er i varselssteg med grunnlag`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.VARSEL,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
        )

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.FAKTA
        aktivtBehandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal ikke gjenoppta behandling når behandling er i grunnlagssteg uten grunnlag`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )

        stegService.gjenopptaSteg(behandlingId)

        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
        aktivtBehandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.VENTER
        aktivtBehandlingsstegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        aktivtBehandlingsstegstilstand.tidsfrist shouldBe LocalDate.now()
            .plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling når behandling er i grunnlagssteg med grunnlag`() {
        lagBehandlingsstegstilstand(
            Behandlingssteg.GRUNNLAG,
            Behandlingsstegstatus.VENTER,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.FAKTA
        aktivtBehandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling når behandling er i vilkårsvurderingssteg`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.VENTER,
            Venteårsak.AVVENTER_DOKUMENTASJON,
        )

        stegService.gjenopptaSteg(behandlingId)

        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.behandlingssteg shouldBe Behandlingssteg.VILKÅRSVURDERING
        aktivtBehandlingsstegstilstand.behandlingsstegsstatus shouldBe Behandlingsstegstatus.KLAR
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
    }

    @Test
    fun `kanAnsvarligSaksbehandlerOppdateres skal returnere true når behandling er sendt til beslutter`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        val behandlingsstegDto =
            BehandlingsstegForeslåVedtaksstegDto(FritekstavsnittDto(perioderMedTekst = emptyList()))
        stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)
            .shouldBeTrue()
    }

    @Test
    fun `kanAnsvarligSaksbehandlerOppdateres skal returnere false når beslutter underkjenner vedtak`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val behandlingsstegDto = lagBehandlingsstegFatteVedtaksstegDto(godkjent = false)
        stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)
            .shouldBeFalse()
    }

    @Test
    fun `kanAnsvarligSaksbehandlerOppdateres skal returnere false når beslutter godkjenner vedtak`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val behandlingsstegDto = lagBehandlingsstegFatteVedtaksstegDto(godkjent = true)
        stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)
            .shouldBeFalse()
    }

    @Test
    fun `kanAnsvarligSaksbehandlerOppdateres skal returnere true når saksbehandler utfører vilkårsvurderingssteg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val behandlingsstegDto = lagBehandlingsstegVilkårsvurderingDto(
            Datoperiode(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 31),
            ),
        )
        stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)
            .shouldBeTrue()
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
    ) {
        val tidsfrist: LocalDate? = venteårsak?.let { LocalDate.now().plusWeeks(it.defaultVenteTidIUker) }
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
                venteårsak = venteårsak,
                tidsfrist = tidsfrist,
                behandlingId = behandlingId,
            ),
        )
    }

    private fun lagBehandlingsstegFaktaDto(): BehandlingsstegFaktaDto {
        val faktaFeilutbetaltePerioderDto = FaktaFeilutbetalingsperiodeDto(
            periode = Datoperiode(FOM, TOM),
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        return BehandlingsstegFaktaDto(
            feilutbetaltePerioder = listOf(faktaFeilutbetaltePerioderDto),
            begrunnelse = "testverdi",
        )
    }

    private fun lagBehandlingsstegVilkårsvurderingDto(periode: Datoperiode): BehandlingsstegVilkårsvurderingDto {
        return BehandlingsstegVilkårsvurderingDto(
            listOf(
                VilkårsvurderingsperiodeDto(
                    periode,
                    Vilkårsvurderingsresultat.GOD_TRO,
                    "Vilkårsvurdering begrunnelse",
                    GodTroDto(
                        false,
                        null,
                        "God tro begrunnelse",
                    ),
                ),
            ),
        )
    }

    private fun lagBehandlingsstegFatteVedtaksstegDto(godkjent: Boolean): BehandlingsstegFatteVedtaksstegDto {
        return BehandlingsstegFatteVedtaksstegDto(
            listOf(
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FAKTA,
                    godkjent = godkjent,
                    begrunnelse = "fakta totrinn begrunnelse",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORELDELSE,
                    godkjent = godkjent,
                    begrunnelse = "foreldelse totrinn begrunnelse",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                    godkjent = godkjent,
                    begrunnelse = "vilkårsvurdering totrinn begrunnelse",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                    godkjent = godkjent,
                    begrunnelse = "foreslåvedtak totrinn begrunnelse",
                ),
            ),
        )
    }

    private fun assertBehandlingssteg(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.shouldHaveSingleElement {
            behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }
    }

    private fun assertFaktadata(behandlingsstegFaktaDto: BehandlingsstegFaktaDto) {
        val faktaFeilutbetaling = faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
        faktaFeilutbetaling.shouldNotBeNull()
        val faktaFeilutbetalingsperioder = faktaFeilutbetaling.perioder.toList()
        faktaFeilutbetalingsperioder.size shouldBe 1
        val faktaFeilutbetaltePerioderDto: FaktaFeilutbetalingsperiodeDto =
            behandlingsstegFaktaDto.feilutbetaltePerioder[0]
        faktaFeilutbetalingsperioder[0].periode.toDatoperiode() shouldBe faktaFeilutbetaltePerioderDto.periode
        faktaFeilutbetalingsperioder[0].hendelsestype shouldBe faktaFeilutbetaltePerioderDto.hendelsestype
        faktaFeilutbetalingsperioder[0].hendelsesundertype shouldBe faktaFeilutbetaltePerioderDto.hendelsesundertype
        "testverdi" shouldBe faktaFeilutbetaling.begrunnelse
    }

    private fun assertForeldelsesdata(foreldelsesperiodeDto: ForeldelsesperiodeDto) {
        val vurdertForeldelsesdata = foreldelseService.hentVurdertForeldelse(behandlingId)
        vurdertForeldelsesdata.foreldetPerioder.size shouldBe 1
        val vurdertForeldetData = vurdertForeldelsesdata.foreldetPerioder[0]
        vurdertForeldetData.begrunnelse shouldBe foreldelsesperiodeDto.begrunnelse
        vurdertForeldetData.foreldelsesvurderingstype shouldBe foreldelsesperiodeDto.foreldelsesvurderingstype
        vurdertForeldetData.feilutbetaltBeløp shouldBe BigDecimal("10000")
        vurdertForeldetData.periode shouldBe foreldelsesperiodeDto.periode
    }

    private fun assertBehandlingsstatus(behandlingId: UUID, behandlingsstatus: Behandlingsstatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandling.status shouldBe behandlingsstatus
    }

    private fun assertOppgave(tasktype: String, forventet: Int = 1) {
        val taskene = taskService.finnTasksMedStatus(
            status = listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET,
                Status.BEHANDLER,
                Status.FERDIG,
            ),
            page = Pageable.unpaged(),
        )
            .filter { tasktype == it.type }

        taskene.size shouldBe forventet
    }

    private fun assertHistorikkTask(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
    ) {
        taskService.finnTasksMedStatus(
            listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET,
                Status.BEHANDLER,
                Status.FERDIG,
            ),
            page = Pageable.unpaged(),
        ).any {
            LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                aktør.name == it.metadata["aktør"] &&
                behandlingId.toString() == it.payload
        }.shouldBeTrue()
    }
}
