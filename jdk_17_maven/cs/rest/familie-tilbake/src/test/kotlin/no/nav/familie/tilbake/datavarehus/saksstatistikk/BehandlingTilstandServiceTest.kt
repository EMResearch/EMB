package no.nav.familie.tilbake.datavarehus.saksstatistikk

import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class BehandlingTilstandServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private lateinit var service: BehandlingTilstandService

    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        service = BehandlingTilstandService(
            behandlingRepository,
            behandlingsstegstilstandRepository,
            fagsakRepository,
            taskService,
            faktaFeilutbetalingService,
        )

        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for nyopprettet behandling`() {
        val behandling = behandlingService.opprettBehandling(
            lagOpprettTilbakekrevingRequest(
                true,
                OPPRETT_TILBAKEKREVING_MED_VARSEL,
            ),
        )
        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.saksnummer shouldBe "1234567"
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe Behandlingstype.TILBAKEKREVING
        tilstand.behandlingsstatus shouldBe Behandlingsstatus.UTREDES
        tilstand.behandlingsresultat shouldBe Behandlingsresultatstype.IKKE_FASTSATT

        tilstand.venterPåBruker shouldBe true
        tilstand.venterPåØkonomi shouldBe false
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe behandling.ansvarligBeslutter
        tilstand.ansvarligSaksbehandler shouldBe behandling.ansvarligSaksbehandler
        tilstand.ansvarligEnhet shouldBe behandling.behandlendeEnhet
        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("1500")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for nyopprettet behandling uten varsel`() {
        val behandling = behandlingService.opprettBehandling(
            lagOpprettTilbakekrevingRequest(
                false,
                OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            ),
        )
        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.saksnummer shouldBe "1234567"
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe Behandlingstype.TILBAKEKREVING
        tilstand.behandlingsstatus shouldBe Behandlingsstatus.UTREDES
        tilstand.behandlingsresultat shouldBe Behandlingsresultatstype.IKKE_FASTSATT

        tilstand.venterPåBruker shouldBe false
        tilstand.venterPåØkonomi shouldBe true
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe behandling.ansvarligBeslutter
        tilstand.ansvarligSaksbehandler shouldBe behandling.ansvarligSaksbehandler
        tilstand.ansvarligEnhet shouldBe behandling.behandlendeEnhet
        tilstand.totalFeilutbetaltBeløp.shouldBeNull()
        tilstand.totalFeilutbetaltPeriode.shouldBeNull()
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for fattet behandling`() {
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.FULL_TILBAKEBETALING)
        val fattetBehandling = behandling.copy(
            behandlendeEnhet = "1234",
            behandlendeEnhetsNavn = "foo bar",
            ansvarligSaksbehandler = "Z111111",
            ansvarligBeslutter = "Z111112",
            resultater = setOf(behandlingsresultat),
        )
        behandlingRepository.update(fattetBehandling)
        behandlingsstegstilstandRepository.insert(Testdata.behandlingsstegstilstand.copy(behandlingssteg = Behandlingssteg.FATTE_VEDTAK))
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.saksnummer shouldBe Testdata.fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe behandling.type
        tilstand.behandlingsstatus shouldBe behandling.status
        tilstand.behandlingsresultat shouldBe behandlingsresultat.type
        tilstand.venterPåBruker shouldBe false
        tilstand.venterPåØkonomi shouldBe false
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe "Z111112"
        tilstand.ansvarligSaksbehandler shouldBe "Z111111"
        tilstand.ansvarligEnhet shouldBe "1234"
        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("10000.00")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingstilstand for behandling på vent`() {
        behandlingsstegstilstandRepository.insert(Testdata.behandlingsstegstilstand)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandlingService.settBehandlingPåVent(
            behandling.id,
            BehandlingPåVentDto(
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                LocalDate.now().plusDays(1),
            ),
        )
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.saksnummer shouldBe Testdata.fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe behandling.type
        tilstand.behandlingsstatus shouldBe behandling.status
        tilstand.behandlingsresultat shouldBe Testdata.behandlingsresultat.type
        tilstand.venterPåBruker shouldBe true
        tilstand.venterPåØkonomi shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(
            OffsetDateTime.now().minusMinutes(1),
            OffsetDateTime.now().plusSeconds(1),
        )

        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("10000.00")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    private fun lagOpprettTilbakekrevingRequest(
        finnesVarsel: Boolean,
        tilbakekrevingsvalg: Tilbakekrevingsvalg,
    ): OpprettTilbakekrevingRequest {
        val fom = YearMonth.now().minusMonths(1).atDay(1)
        val tom = YearMonth.now().atEndOfMonth()

        val varsel = if (finnesVarsel) {
            Varsel(
                varseltekst = "testverdi",
                sumFeilutbetaling = BigDecimal.valueOf(1500L),
                perioder = listOf(Periode(fom, tom)),
            )
        } else {
            null
        }

        val faktainfo = Faktainfo(
            revurderingsårsak = "testverdi",
            revurderingsresultat = "testresultat",
            tilbakekrevingsvalg = tilbakekrevingsvalg,
        )

        return OpprettTilbakekrevingRequest(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            eksternFagsakId = "1234567",
            personIdent = "321321322",
            eksternId = UUID.randomUUID().toString(),
            manueltOpprettet = false,
            språkkode = Språkkode.NN,
            enhetId = "8020",
            enhetsnavn = "Oslo",
            varsel = varsel,
            revurderingsvedtaksdato = fom,
            faktainfo = faktainfo,
            saksbehandlerIdent = "Z0000",
        )
    }
}
