package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Properties

internal class AutomatiskSaksbehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelsesRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var sendØkonomiTilbakekrevingsvedtakTask: SendØkonomiTilbakekrevingsvedtakTask

    @Autowired
    private lateinit var sendVedtaksbrevTask: SendVedtaksbrevTask

    @Autowired
    private lateinit var avsluttBehandlingTask: AvsluttBehandlingTask

    private val taskService: TaskService = mockk()

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var automatiskSaksbehandlingTask: AutomatiskSaksbehandlingTask

    private val fagsak: Fagsak = Testdata.fagsak
    private val behandling: Behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        val fagsystemsbehandling = behandling.aktivFagsystemsbehandling.copy(
            tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
        )
        behandlingRepository.insert(
            behandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                status = Behandlingsstatus.UTREDES,
            ),
        )
        val feilKravgrunnlagBeløp = Testdata.feilKravgrunnlagsbeløp433.copy(nyttBeløp = BigDecimal("100"))
        val ytelKravgrunnlagsbeløp433 =
            Testdata.ytelKravgrunnlagsbeløp433.copy(
                opprinneligUtbetalingsbeløp = BigDecimal("100"),
                tilbakekrevesBeløp = BigDecimal("100"),
            )

        val kravgrunnlag = Testdata.kravgrunnlag431
            .copy(
                kontrollfelt = "2019-11-22-19.09.31.458065",
                perioder = setOf(
                    Testdata.kravgrunnlagsperiode432.copy(
                        beløp = setOf(
                            feilKravgrunnlagBeløp,
                            ytelKravgrunnlagsbeløp433,
                        ),
                    ),
                ),
            )

        kravgrunnlagRepository.insert(kravgrunnlag)
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.GRUNNLAG,
                Behandlingsstegstatus.UTFØRT,
            ),
        )
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.FAKTA,
                Behandlingsstegstatus.KLAR,
            ),
        )
    }

    @Test
    fun `doTask skal ikke behandle når behandling allerede er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id).copy(status = Behandlingsstatus.AVSLUTTET)
        behandlingRepository.update(behandling)

        val exception = shouldThrow<RuntimeException> { automatiskSaksbehandlingTask.doTask(lagTask()) }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet"
    }

    @Test
    fun `doTask skal ikke behandle når behandling er på vent`() {
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.ENDRE_TILKJENT_YTELSE,
            LocalDate.now().plusWeeks(2),
        )

        val exception = shouldThrow<RuntimeException> { automatiskSaksbehandlingTask.doTask(lagTask()) }
        exception.message shouldBe "Behandling med id=${behandling.id} er på vent, kan ikke behandle steg FAKTA"
    }

    @Test
    fun `doTask skal behandle behandling automatisk`() {
        automatiskSaksbehandlingTask.doTask(lagTask())
        mockTaskExecution()

        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.saksbehandlingstype shouldBe Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP
        behandling.ansvarligSaksbehandler shouldBe "VL"
        behandling.ansvarligBeslutter shouldBe "VL"
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.IVERKSETT_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.AVSLUTTET, Behandlingsstegstatus.UTFØRT)

        val faktaFeilutbetaling = faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        faktaFeilutbetaling.shouldNotBeNull()
        faktaFeilutbetaling.begrunnelse shouldBe Constants.AUTOMATISK_SAKSBEHANDLING_BEGUNNLESE
        faktaFeilutbetaling.perioder.shouldHaveSingleElement {
            Hendelsestype.ANNET == it.hendelsestype &&
                Hendelsesundertype.ANNET_FRITEKST == it.hendelsesundertype
        }

        foreldelsesRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        vilkårsvurdering.shouldNotBeNull()
        vilkårsvurdering.perioder.shouldHaveSingleElement {
            Constants.AUTOMATISK_SAKSBEHANDLING_BEGUNNLESE == it.begrunnelse &&
                Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT == it.vilkårsvurderingsresultat &&
                it.aktsomhet != null && it.aktsomhet!!.aktsomhet == Aktsomhet.SIMPEL_UAKTSOMHET
            !it.aktsomhet!!.tilbakekrevSmåbeløp
        }

        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandling.id).shouldBeNull()
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ): Behandlingsstegstilstand {
        return Behandlingsstegstilstand(
            behandlingId = behandling.id,
            behandlingssteg = behandlingssteg,
            behandlingsstegsstatus = behandlingsstegstatus,
        )
    }

    private fun lagTask(): Task {
        return Task(type = AutomatiskSaksbehandlingTask.TYPE, payload = behandling.id.toString())
    }

    private fun assertBehandlingsstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.any {
            behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }.shouldBeTrue()
    }

    private fun mockTaskExecution() {
        val sendVedtakTilØkonomiTask = Task(
            type = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                setProperty(
                    "ansvarligSaksbehandler",
                    ContextService.hentSaksbehandler(),
                )
            },
        )
        every { taskService.save(sendVedtakTilØkonomiTask) }.run {
            sendØkonomiTilbakekrevingsvedtakTask.doTask(sendVedtakTilØkonomiTask)
        }

        val vedtaksbrevTask = Task(type = SendVedtaksbrevTask.TYPE, payload = behandling.id.toString())
        every { taskService.save(vedtaksbrevTask) }.run {
            sendVedtaksbrevTask.doTask(vedtaksbrevTask)
        }

        val avsluttTask = Task(type = AvsluttBehandlingTask.TYPE, payload = behandling.id.toString())
        every { taskService.save(avsluttTask) }.run { avsluttBehandlingTask.doTask(avsluttTask) }
    }
}
