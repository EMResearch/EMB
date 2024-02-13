package no.nav.familie.tilbake.beregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.bigdecimal.shouldBeZero
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.data.Testdata.lagFeilBeløp
import no.nav.familie.tilbake.data.Testdata.lagYtelBeløp
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TilbakekrevingsberegningServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilbakekrevingsberegningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som ikke er foreldet`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.periode shouldBe periode
        r.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        r.vurdering shouldBe Aktsomhet.FORSETT
        r.renteprosent shouldBe BigDecimal.valueOf(10)
        r.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        r.manueltSattTilbakekrevingsbeløp shouldBe null
        r.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `hentBeregningsresultat skal hente beregningsresultat for periode som ikke er foreldet`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)

        val beregningsresultat = tilbakekrevingsberegningService.hentBeregningsresultat(Testdata.behandling.id)
        beregningsresultat.beregningsresultatsperioder.size shouldBe 1
        val beregningsresultatsperiode = beregningsresultat.beregningsresultatsperioder[0]
        beregningsresultatsperiode.periode shouldBe periode.toDatoperiode()
        beregningsresultatsperiode.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        beregningsresultatsperiode.vurdering shouldBe Aktsomhet.FORSETT
        beregningsresultatsperiode.renteprosent shouldBe BigDecimal.valueOf(10)
        beregningsresultatsperiode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        beregningsresultatsperiode.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som er foreldet`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.FORELDET, periode.fom.plusMonths(8).atDay(1))
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.periode shouldBe periode
        r.tilbakekrevingsbeløp.shouldBeZero()
        r.vurdering shouldBe AnnenVurdering.FORELDET
        r.renteprosent shouldBe null
        r.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        r.manueltSattTilbakekrevingsbeløp shouldBe null
        r.andelAvBeløp shouldBe BigDecimal.ZERO
        r.rentebeløp.shouldBeZero()
        r.tilbakekrevingsbeløpUtenRenter.shouldBeZero()
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.INGEN_TILBAKEBETALING
    }

    @Test
    fun `hentBeregningsresultat skal hente beregningsresultat for periode som er foreldet`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.FORELDET, periode.fom.plusMonths(8).atDay(1))

        val beregningsresultat = tilbakekrevingsberegningService.hentBeregningsresultat(Testdata.behandling.id)
        beregningsresultat.beregningsresultatsperioder.size shouldBe 1
        val beregningsresultatsperiode = beregningsresultat.beregningsresultatsperioder[0]
        beregningsresultatsperiode.periode shouldBe periode.toDatoperiode()
        beregningsresultatsperiode.tilbakekrevingsbeløp.shouldNotBeNull()
        beregningsresultatsperiode.tilbakekrevingsbeløp!!.shouldBeZero()
        beregningsresultatsperiode.vurdering shouldBe AnnenVurdering.FORELDET
        beregningsresultatsperiode.renteprosent shouldBe null
        beregningsresultatsperiode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        beregningsresultatsperiode.andelAvBeløp shouldBe BigDecimal.ZERO
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.INGEN_TILBAKEBETALING
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som ikke er foreldet med skattProsent`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.valueOf(10))
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.periode shouldBe periode
        r.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        r.vurdering shouldBe Aktsomhet.FORSETT
        r.renteprosent shouldBe BigDecimal.valueOf(10)
        r.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        r.manueltSattTilbakekrevingsbeløp shouldBe null
        r.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        r.skattebeløp shouldBe BigDecimal.valueOf(1000)
        r.tilbakekrevingsbeløpEtterSkatt shouldBe BigDecimal.valueOf(10000)
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `hentBeregningsresultat skal hente beregningsresultat for periode som ikke er foreldet med skattProsent`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.valueOf(10))
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)

        val beregningsresultat = tilbakekrevingsberegningService.hentBeregningsresultat(Testdata.behandling.id)
        beregningsresultat.beregningsresultatsperioder.size shouldBe 1
        val beregningsresultatsperiode = beregningsresultat.beregningsresultatsperioder[0]
        beregningsresultatsperiode.periode shouldBe periode.toDatoperiode()
        beregningsresultatsperiode.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        beregningsresultatsperiode.vurdering shouldBe Aktsomhet.FORSETT
        beregningsresultatsperiode.renteprosent shouldBe BigDecimal.valueOf(10)
        beregningsresultatsperiode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        beregningsresultatsperiode.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        beregningsresultatsperiode.tilbakekrevesBeløpEtterSkatt shouldBe BigDecimal.valueOf(10000)
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `beregn skalberegne riktig beløp og utbetalt beløp for periode`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.valueOf(10))
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.utbetaltYtelsesbeløp shouldBe BigDecimal.valueOf(10000)
        r.riktigYtelsesbeløp shouldBe BigDecimal.ZERO
    }

    @Test
    fun `beregn skal beregne riktige beløp ved delvis feilutbetaling for perioder sammenslått til en logisk periode`() {
        val skatteprosent = BigDecimal.valueOf(10)
        val periode1 = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        val periode2 = Månedsperiode(LocalDate.of(2019, 5, 4), LocalDate.of(2019, 5, 6))
        val logiskPeriode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 6))
        val utbetalt1 = BigDecimal.valueOf(10000)
        val nyttBeløp1 = BigDecimal.valueOf(5000)
        val utbetalt2 = BigDecimal.valueOf(10000)
        val nyttBeløp2 = BigDecimal.valueOf(100)
        val feilutbetalt2 = utbetalt2.subtract(nyttBeløp2)
        val feilutbetalt1 = utbetalt1.subtract(nyttBeløp1)
        val grunnlagPeriode1: Kravgrunnlagsperiode432 =
            lagGrunnlagPeriode(
                periode1,
                1000,
                setOf(
                    lagYtelBeløp(utbetalt1, nyttBeløp1, skatteprosent),
                    lagFeilBeløp(feilutbetalt1),
                ),
            )
        val grunnlagPeriode2: Kravgrunnlagsperiode432 =
            lagGrunnlagPeriode(
                periode2,
                1000,
                setOf(
                    lagYtelBeløp(utbetalt2, nyttBeløp2, skatteprosent),
                    lagFeilBeløp(feilutbetalt2),
                ),
            )
        val grunnlag: Kravgrunnlag431 = lagGrunnlag(setOf(grunnlagPeriode1, grunnlagPeriode2))
        kravgrunnlagRepository.insert(grunnlag)
        lagForeldelse(Testdata.behandling.id, logiskPeriode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, logiskPeriode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.periode shouldBe logiskPeriode
        r.utbetaltYtelsesbeløp shouldBe utbetalt1.add(utbetalt2)
        r.riktigYtelsesbeløp shouldBe nyttBeløp1.add(nyttBeløp2)
    }

    @Test
    fun `beregn skal beregne tilbakekrevingsbeløp for ikkeForeldetPeriode når beregnetPeriode er på tvers av grunnlagPeriode`() {
        val periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
        val periode1 = Månedsperiode(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30))
        val logiskPeriode = Månedsperiode(
            LocalDate.of(2019, 5, 1),
            LocalDate.of(2019, 6, 30),
        )
        val grunnlagPeriode: Kravgrunnlagsperiode432 =
            lagGrunnlagPeriode(
                periode,
                1000,
                setOf(
                    lagYtelBeløp(BigDecimal.valueOf(10000), BigDecimal.valueOf(10)),
                    lagFeilBeløp(BigDecimal.valueOf(10000)),
                ),
            )
        val grunnlagPeriode1: Kravgrunnlagsperiode432 =
            lagGrunnlagPeriode(
                periode1,
                1000,
                setOf(
                    lagYtelBeløp(
                        BigDecimal.valueOf(10000),
                        BigDecimal.valueOf(10),
                    ),
                    lagFeilBeløp(BigDecimal.valueOf(10000)),
                ),
            )
        val grunnlag: Kravgrunnlag431 = lagGrunnlag(setOf(grunnlagPeriode, grunnlagPeriode1))
        kravgrunnlagRepository.insert(grunnlag)
        lagForeldelse(Testdata.behandling.id, logiskPeriode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, logiskPeriode)

        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        resultat.shouldHaveSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        r.periode shouldBe logiskPeriode
        r.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(22000)
        r.vurdering shouldBe Aktsomhet.FORSETT
        r.renteprosent shouldBe BigDecimal.valueOf(10)
        r.feilutbetaltBeløp shouldBe BigDecimal.valueOf(20000)
        r.manueltSattTilbakekrevingsbeløp shouldBe null
        r.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        r.skattebeløp shouldBe BigDecimal.valueOf(2000)
        r.tilbakekrevingsbeløpEtterSkatt shouldBe BigDecimal.valueOf(20000)
        beregningsresultat.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `beregnBeløp skal beregne feilutbetaltBeløp når saksbehandler deler opp periode`() {
        val kravgrunnlag431 = Testdata.kravgrunnlag431
        val feilkravgrunnlagsbeløp = Testdata.feilKravgrunnlagsbeløp433
        val yteseskravgrunnlagsbeløp = Testdata.ytelKravgrunnlagsbeløp433
        val førsteKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
            .copy(
                periode = Månedsperiode(YearMonth.of(2017, 1), YearMonth.of(2017, 1)),
                beløp = setOf(
                    feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                ),
            )
        val andreKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
            .copy(
                id = UUID.randomUUID(),
                periode = Månedsperiode(YearMonth.of(2017, 2), YearMonth.of(2017, 2)),
                beløp = setOf(
                    feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                ),
            )
        kravgrunnlagRepository.insert(
            kravgrunnlag431.copy(
                perioder = setOf(
                    førsteKravgrunnlagsperiode,
                    andreKravgrunnlagsperiode,
                ),
            ),
        )

        val beregnetPerioderDto = tilbakekrevingsberegningService.beregnBeløp(
            behandlingId = Testdata.behandling.id,
            perioder = listOf(
                Datoperiode(
                    LocalDate.of(
                        2017,
                        1,
                        1,
                    ),
                    LocalDate.of(
                        2017,
                        1,
                        31,
                    ),
                ),
                Datoperiode(
                    LocalDate.of(
                        2017,
                        2,
                        1,
                    ),
                    LocalDate.of(
                        2017,
                        2,
                        28,
                    ),
                ),
            ),
        )
        beregnetPerioderDto.beregnetPerioder.size shouldBe 2
        beregnetPerioderDto.beregnetPerioder[0].periode shouldBe Datoperiode(LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 31))
        beregnetPerioderDto.beregnetPerioder[0].feilutbetaltBeløp shouldBe BigDecimal("10000")
        beregnetPerioderDto.beregnetPerioder[1].periode shouldBe Datoperiode(LocalDate.of(2017, 2, 1), LocalDate.of(2017, 2, 28))
        beregnetPerioderDto.beregnetPerioder[1].feilutbetaltBeløp shouldBe BigDecimal("10000")
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke starter første dato`() {
        val exception = shouldThrow<RuntimeException> {
            tilbakekrevingsberegningService.beregnBeløp(
                behandlingId = Testdata.behandling.id,
                perioder = listOf(
                    Datoperiode(
                        LocalDate.of(2017, 1, 1),
                        LocalDate.of(2017, 1, 31),
                    ),
                    Datoperiode(
                        LocalDate.of(2017, 2, 16),
                        LocalDate.of(2017, 2, 28),
                    ),
                ),
            )
        }
        exception.message shouldBe "Periode med ${
            Datoperiode(
                LocalDate.of(2017, 2, 16),
                LocalDate.of(2017, 2, 28),
            )
        } er ikke i hele måneder"
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke slutter siste dato`() {
        val exception = shouldThrow<RuntimeException> {
            tilbakekrevingsberegningService.beregnBeløp(
                behandlingId = Testdata.behandling.id,
                perioder = listOf(
                    Datoperiode(
                        LocalDate.of(2017, 1, 1),
                        LocalDate.of(2017, 1, 27),
                    ),
                    Datoperiode(
                        LocalDate.of(2017, 2, 1),
                        LocalDate.of(2017, 2, 28),
                    ),
                ),
            )
        }
        exception.message shouldBe "Periode med ${
            Datoperiode(
                LocalDate.of(2017, 1, 1),
                LocalDate.of(2017, 1, 27),
            )
        } er ikke i hele måneder"
    }

    private fun lagVilkårsvurderingMedForsett(behandlingId: UUID, vararg perioder: Månedsperiode) {
        val vurderingsperioder = perioder.map {
            Vilkårsvurderingsperiode(
                periode = Månedsperiode(it.fom, it.tom),
                begrunnelse = "foo",
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo",
                ),
            )
        }.toSet()
        val vurdering = Vilkårsvurdering(
            behandlingId = behandlingId,
            perioder = vurderingsperioder,
        )

        vilkårsvurderingRepository.insert(vurdering)
    }

    private fun lagForeldelse(
        behandlingId: UUID,
        periode: Månedsperiode,
        resultat: Foreldelsesvurderingstype,
        foreldelsesFrist: LocalDate?,
    ) {
        val vurdertForeldelse =
            VurdertForeldelse(
                behandlingId = behandlingId,
                foreldelsesperioder = setOf(
                    Foreldelsesperiode(
                        periode = periode,
                        begrunnelse = "foo",
                        foreldelsesvurderingstype = resultat,
                        foreldelsesfrist = foreldelsesFrist,
                    ),
                ),
            )
        vurdertForeldelseRepository.insert(vurdertForeldelse)
    }

    private fun lagKravgrunnlag(periode: Månedsperiode, skattProsent: BigDecimal) {
        val p = Testdata.kravgrunnlagsperiode432.copy(
            id = UUID.randomUUID(),
            periode = periode,
            beløp = setOf(
                lagFeilBeløp(BigDecimal.valueOf(10000)),
                lagYtelBeløp(BigDecimal.valueOf(10000), skattProsent),
            ),
        )
        val grunnlag: Kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(p))
        kravgrunnlagRepository.insert(grunnlag)
    }

    private fun lagGrunnlagPeriode(
        periode: Månedsperiode,
        skattMnd: Int,
        beløp: Set<Kravgrunnlagsbeløp433> = setOf(),
    ): Kravgrunnlagsperiode432 {
        return Kravgrunnlagsperiode432(
            periode = periode,
            månedligSkattebeløp = BigDecimal.valueOf(skattMnd.toLong()),
            beløp = beløp,
        )
    }

    private fun lagGrunnlag(perioder: Set<Kravgrunnlagsperiode432>): Kravgrunnlag431 {
        return Testdata.kravgrunnlag431.copy(perioder = perioder)
    }
}
