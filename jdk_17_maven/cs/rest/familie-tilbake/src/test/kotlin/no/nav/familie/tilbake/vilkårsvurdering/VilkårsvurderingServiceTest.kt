package no.nav.familie.tilbake.vilkårsvurdering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.AktivitetDto
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.GodTroDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class VilkårsvurderingServiceTest : OppslagSpringRunnerTest() {

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
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        val førstePeriode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(fom = YearMonth.of(2020, 1), tom = YearMonth.of(2020, 1)),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                ),
            )
        val andrePeriode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(fom = YearMonth.of(2020, 2), tom = YearMonth.of(2020, 2)),
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                ),
            )

        val kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode))
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val periode = FaktaFeilutbetalingsperiode(
            periode = Månedsperiode(førstePeriode.periode.fom, andrePeriode.periode.tom),
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandling.id,
                begrunnelse = "fakta begrunnelse",
                perioder = setOf(periode),
            ),
        )

        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra fakta perioder`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        vurdertPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse.shouldBeNull()
        vurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra foreldelse perioder som ikke er foreldet`() {
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2

        val foreldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        foreldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        foreldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        foreldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        foreldetPeriode.foreldet.shouldBeTrue()
        foreldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(foreldetPeriode.aktiviteter)
        foreldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        foreldetPeriode.begrunnelse shouldBe "foreldelse begrunnelse 1"
        foreldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()

        val ikkeForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        ikkeForeldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        ikkeForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        ikkeForeldetPeriode.foreldet.shouldBeFalse()
        ikkeForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        ikkeForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        ikkeForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(ikkeForeldetPeriode.aktiviteter)
        ikkeForeldetPeriode.begrunnelse.shouldBeNull()
        ikkeForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering når perioder er delt opp`() {
        // delt opp i to perioder
        val periode1 = Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val periode2 = Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        val behandlingsstegVilkårsvurderingDto = lagVilkårsvurderingMedGodTro(perioder = listOf(periode1, periode2))
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, behandlingsstegVilkårsvurderingDto)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2

        val førstePeriode = vurdertVilkårsvurderingDto.perioder[0]
        førstePeriode.periode shouldBe periode1
        førstePeriode.hendelsestype shouldBe Hendelsestype.ANNET
        førstePeriode.feilutbetaltBeløp shouldBe BigDecimal(10000)
        førstePeriode.foreldet.shouldBeFalse()
        assertAktiviteter(førstePeriode.aktiviteter)
        førstePeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        var vilkårsvurderingsresultatDto = førstePeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO

        val andrePeriode = vurdertVilkårsvurderingDto.perioder[1]
        andrePeriode.periode shouldBe periode2
        andrePeriode.hendelsestype shouldBe Hendelsestype.ANNET
        andrePeriode.feilutbetaltBeløp shouldBe BigDecimal(10000)
        andrePeriode.foreldet.shouldBeFalse()
        assertAktiviteter(andrePeriode.aktiviteter)
        andrePeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        vilkårsvurderingsresultatDto = andrePeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering med reduserte beløper`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        val justBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.JUST,
            nyttBeløp = BigDecimal(5000),
            opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
        )
        val trekBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.TREK,
            nyttBeløp = BigDecimal.ZERO,
            opprinneligUtbetalingsbeløp = BigDecimal(-2000),
        )
        val skatBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.SKAT,
            nyttBeløp = BigDecimal.ZERO,
            opprinneligUtbetalingsbeløp = BigDecimal(-2000),
        )
        val førstePeriode = kravgrunnlag431.perioder
            .toList()[0]
            .copy(
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    justBeløp,
                ),
            )
        val andrePeriode = kravgrunnlag431.perioder
            .toList()[1]
            .copy(
                beløp = setOf(
                    Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                    trekBeløp,
                    skatBeløp,
                ),
            )
        kravgrunnlagRepository.update(kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode)))

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()

        vurdertPeriode.reduserteBeløper.shouldNotBeEmpty()
        vurdertPeriode.reduserteBeløper.size shouldBe 3
        var redusertBeløp = vurdertPeriode.reduserteBeløper[0]
        redusertBeløp.trekk.shouldBeTrue()
        redusertBeløp.beløp shouldBe BigDecimal("2000.00")
        redusertBeløp = vurdertPeriode.reduserteBeløper[1]
        redusertBeløp.trekk.shouldBeTrue()
        redusertBeløp.beløp shouldBe BigDecimal("2000.00")
        redusertBeløp = vurdertPeriode.reduserteBeløper[2]
        redusertBeløp.trekk.shouldBeFalse()
        redusertBeløp.beløp shouldBe BigDecimal("5000.00")

        vurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        vurdertPeriode.begrunnelse.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret simpel aktsomhet vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET))
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
        )

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.godTro.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT
        val aktsomhetDto = vilkårsvurderingsresultatDto.aktsomhet
        aktsomhetDto.shouldNotBeNull()
        aktsomhetDto.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        aktsomhetDto.begrunnelse shouldBe "Aktsomhet begrunnelse"
        aktsomhetDto.tilbakekrevSmåbeløp.shouldBeFalse()
        aktsomhetDto.særligeGrunnerTilReduksjon.shouldBeFalse()
        aktsomhetDto.andelTilbakekreves.shouldBeNull()
        aktsomhetDto.ileggRenter.shouldBeNull()
        aktsomhetDto.beløpTilbakekreves.shouldBeNull()
        aktsomhetDto.særligeGrunnerBegrunnelse shouldBe "Særlig grunner begrunnelse"
        val særligGrunner = aktsomhetDto.særligeGrunner
        særligGrunner.shouldNotBeNull()
        særligGrunner.any { SærligGrunn.GRAD_AV_UAKTSOMHET == it.særligGrunn }.shouldBeTrue()
        særligGrunner.all { it.begrunnelse == null }.shouldBeTrue()
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret god tro vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedGodTro(perioder = listOf(Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))))
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
        )

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Constants.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.aktsomhet.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        godTroDto.shouldNotBeNull()
        godTroDto.beløpErIBehold.shouldBeTrue()
        godTroDto.begrunnelse shouldBe "God tro begrunnelse"
        godTroDto.beløpTilbakekreves.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente foreldelse perioder som endret til IKKE_FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 1
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 1

        // behandle vilkårsvurdering
        vilkårsvurderingService
            .lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedGodTro(
                    perioder = listOf(
                        Datoperiode(
                            YearMonth.of(2020, 2),
                            YearMonth.of(2020, 2),
                        ),
                    ),
                ),
            )
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        // endret begge perioder til IKKE_FORELDET
        val vurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId = behandling.id)!!
        oppdaterForeldelsesvurdering(
            vurdertForeldelse,
            Foreldelsesvurderingstype.IKKE_FORELDET,
            Foreldelsesvurderingstype.IKKE_FORELDET,
        )

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 2

        val ikkeVurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        ikkeVurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        ikkeVurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        ikkeVurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        ikkeVurdertPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(ikkeVurdertPeriode.aktiviteter)
        ikkeVurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        ikkeVurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        ikkeVurdertPeriode.begrunnelse.shouldBeNull()

        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[1]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.aktsomhet.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        godTroDto.shouldNotBeNull()
        godTroDto.beløpErIBehold.shouldBeTrue()
        godTroDto.begrunnelse shouldBe "God tro begrunnelse"
        godTroDto.beløpTilbakekreves.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente perioder som endret til FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 1
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 1

        // behandle vilkårsvurdering
        vilkårsvurderingService
            .lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedGodTro(
                    perioder = listOf(
                        Datoperiode(
                            YearMonth.of(2020, 2),
                            YearMonth.of(2020, 2),
                        ),
                    ),
                ),
            )
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        // endret begge perioder til FORELDET
        val vurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId = behandling.id)!!
        oppdaterForeldelsesvurdering(vurdertForeldelse, Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.FORELDET)

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 2

        val førsteForeldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        førsteForeldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        førsteForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        førsteForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        førsteForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(førsteForeldetPeriode.aktiviteter)
        førsteForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        førsteForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        førsteForeldetPeriode.begrunnelse shouldBe "foreldelse begrunnelse 1"

        val andreForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        andreForeldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        andreForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        andreForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        andreForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(andreForeldetPeriode.aktiviteter)
        andreForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        andreForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        andreForeldetPeriode.begrunnelse shouldBe "foreldelse begrunnelse 2"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når andelTilbakekreves er mer enn 100 prosent `() {
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService
                .lagreVilkårsvurdering(
                    behandling.id,
                    lagVilkårsvurderingMedSimpelAktsomhet(
                        andelTilbakekreves = BigDecimal(120),
                        særligGrunn =
                        SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET),
                    ),
                )
        }
        exception.message shouldBe "Andel som skal tilbakekreves kan ikke være mer enn 100 prosent"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når ANNET særlig grunner mangler ANNET begrunnelse`() {
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService
                .lagreVilkårsvurdering(
                    behandling.id,
                    lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunn.ANNET)),
                )
        }
        exception.message shouldBe "ANNET særlig grunner må ha ANNET begrunnelse"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når manueltSattBeløp er mer enn feilutbetalt beløp`() {
        // forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedSimpelAktsomhet(
                manueltSattBeløp = BigDecimal(30000),
                særligGrunn = SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET),
            )
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, behandlingsstegVilkårsvurderingDto)
        }
        exception.message shouldBe "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når tilbakekrevesBeløp er mer enn feilutbetalt beløp`() {
        // forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedGodTro(
                    listOf(
                        Datoperiode(
                            YearMonth.of(2020, 1),
                            YearMonth.of(2020, 2),
                        ),
                    ),
                    BigDecimal(30000),
                ),
            )
        }
        exception.message shouldBe "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
    }

    @Test
    fun `lagreVilkårsvurdering skal lagre vilkårsvurdering med false ileggRenter for barnetrygd behandling`() {
        // forutsetter at behandling opprettet for barnetrygd fra Testdata
        vilkårsvurderingService
            .lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedSimpelAktsomhet(
                    ileggRenter = true,
                    særligGrunn =
                    SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET),
                ),
            )

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        vilkårsvurdering.shouldNotBeNull()

        vilkårsvurdering.perioder.shouldNotBeEmpty()
        vilkårsvurdering.perioder.size shouldBe 1
        val vurdertPeriode = vilkårsvurdering.perioder.toList()[0]
        vurdertPeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        vurdertPeriode.aktsomhet.shouldNotBeNull()
        vurdertPeriode.godTro.shouldBeNull()
        vurdertPeriode.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT

        val aktsomhet = vurdertPeriode.aktsomhet
        aktsomhet.shouldNotBeNull()
        aktsomhet.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        aktsomhet.begrunnelse shouldBe "Aktsomhet begrunnelse"
        aktsomhet.tilbakekrevSmåbeløp.shouldBeFalse()
        aktsomhet.særligeGrunnerTilReduksjon.shouldBeFalse()
        aktsomhet.andelTilbakekreves.shouldBeNull()
        aktsomhet.andelTilbakekreves.shouldBeNull()
        aktsomhet.ileggRenter shouldBe false
        aktsomhet.manueltSattBeløp.shouldBeNull()
        aktsomhet.særligeGrunnerBegrunnelse shouldBe "Særlig grunner begrunnelse"

        val særligGrunner = aktsomhet.vilkårsvurderingSærligeGrunner
        særligGrunner.shouldNotBeNull()
        særligGrunner.any { SærligGrunn.GRAD_AV_UAKTSOMHET == it.særligGrunn }.shouldBeTrue()
        særligGrunner.all { it.begrunnelse == null }.shouldBeTrue()
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg, behandlingsstegstatus: Behandlingsstegstatus) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
                behandlingId = behandling.id,
            ),
        )
    }

    private fun lagKravgrunnlagsbeløp(
        klassetype: Klassetype,
        nyttBeløp: BigDecimal,
        opprinneligUtbetalingsbeløp: BigDecimal,
    ): Kravgrunnlagsbeløp433 {
        return Kravgrunnlagsbeløp433(
            id = UUID.randomUUID(),
            klassetype = klassetype,
            klassekode = Klassekode.BATR,
            opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
            nyttBeløp = nyttBeløp,
            tilbakekrevesBeløp = BigDecimal.ZERO,
            skatteprosent = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal.ZERO,
            resultatkode = "testverdi",
            årsakskode = "testverdi",
            skyldkode = "testverdi",
        )
    }

    private fun assertAktiviteter(aktiviteter: List<AktivitetDto>) {
        aktiviteter.shouldNotBeEmpty()
        aktiviteter.size shouldBe 1
        aktiviteter[0].aktivitet shouldBe Klassekode.BATR.aktivitet
    }

    private fun lagVilkårsvurderingMedSimpelAktsomhet(
        andelTilbakekreves: BigDecimal? = null,
        manueltSattBeløp: BigDecimal? = null,
        ileggRenter: Boolean? = null,
        særligGrunn: SærligGrunnDto,
    ): BehandlingsstegVilkårsvurderingDto {
        val periode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2)),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            begrunnelse = "Vilkårsvurdering begrunnelse",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                andelTilbakekreves = andelTilbakekreves,
                beløpTilbakekreves = manueltSattBeløp,
                ileggRenter = ileggRenter,
                begrunnelse = "Aktsomhet begrunnelse",
                særligeGrunner = listOf(særligGrunn),
                tilbakekrevSmåbeløp = false,
                særligeGrunnerBegrunnelse =
                "Særlig grunner begrunnelse",
            ),
        )
        return BehandlingsstegVilkårsvurderingDto(listOf(periode))
    }

    private fun lagVilkårsvurderingMedGodTro(
        perioder: List<Datoperiode>,
        beløpTilbakekreves: BigDecimal? = null,
    ): BehandlingsstegVilkårsvurderingDto {
        return BehandlingsstegVilkårsvurderingDto(
            vilkårsvurderingsperioder = perioder.map {
                VilkårsvurderingsperiodeDto(
                    periode = it,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                    begrunnelse = "Vilkårsvurdering begrunnelse",
                    godTroDto = GodTroDto(
                        begrunnelse = "God tro begrunnelse",
                        beløpErIBehold = true,
                        beløpTilbakekreves = beløpTilbakekreves,
                    ),
                )
            },
        )
    }

    private fun lagForeldese(vararg foreldelsesvurderingstyper: Foreldelsesvurderingstype) {
        val foreldelsesperioder =
            setOf(
                Foreldelsesperiode(
                    periode = Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)),
                    foreldelsesvurderingstype = foreldelsesvurderingstyper[0],
                    begrunnelse = "foreldelse begrunnelse 1",
                ),
                Foreldelsesperiode(
                    periode = Månedsperiode(YearMonth.of(2020, 2), YearMonth.of(2020, 2)),
                    foreldelsesvurderingstype = foreldelsesvurderingstyper[1],
                    begrunnelse = "foreldelse begrunnelse 2",
                ),
            )
        foreldelseRepository.insert(VurdertForeldelse(behandlingId = behandling.id, foreldelsesperioder = foreldelsesperioder))
    }

    private fun oppdaterForeldelsesvurdering(
        vurdertForeldelse: VurdertForeldelse,
        vararg foreldelsesvurderingstyper: Foreldelsesvurderingstype,
    ) {
        val foreldelsesperioder = setOf(
            Foreldelsesperiode(
                periode = Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)),
                foreldelsesvurderingstype = foreldelsesvurderingstyper[0],
                begrunnelse = "foreldelse begrunnelse 1",
            ),
            Foreldelsesperiode(
                periode = Månedsperiode(YearMonth.of(2020, 2), YearMonth.of(2020, 2)),
                foreldelsesvurderingstype = foreldelsesvurderingstyper[1],
                begrunnelse = "foreldelse begrunnelse 2",
            ),
        )
        foreldelseRepository.update(vurdertForeldelse.copy(foreldelsesperioder = foreldelsesperioder))
    }
}
