package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGeneratorService
import no.nav.familie.ba.sak.integrasjoner.økonomi.lagUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class KontrollerNyUtbetalingsgeneratorServiceTest {

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @MockK
    private lateinit var utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService

    @MockK
    private lateinit var økonomiKlient: ØkonomiKlient

    @MockK
    private lateinit var tikjentYtelseRepository: TilkjentYtelseRepository

    @InjectMockKs
    private lateinit var kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal fange opp at gammel simulering har perioder med endring før ny simulering og ulikt resultat i samme perioder`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(apr(2023), mai(2023), 250),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(2)
        assertThat(
            simuleringsPeriodeDiffFeil.containsAll(
                listOf(
                    DiffFeilType.TidligerePerioderIGammelUlik0,
                    DiffFeilType.UliktResultatISammePeriode,
                ),
            ),
        ).isTrue
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering har perioder uten endring før ny simulering og resultatene er like i øvrige perioder`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 0),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering og ny simulering er helt like`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom gammel simulering og ny simulering har et ulikt resultat`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 300),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 320),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.UliktResultatISammePeriode)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom gammel simulering har endring før ny simulering`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 320),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 320),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.TidligerePerioderIGammelUlik0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering og ny simulering er like og har hull i periodene`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mai(2023), jun(2023), 300),
                Periode(aug(2023), sep(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mai(2023), jun(2023), 300),
                Periode(aug(2023), sep(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke kjøre sammenligning dersom det ikke finnes noen utbetalingsperioder i utbetalingsoppdraget fra gammel`() {
        setupMocks()
        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
            )
        } returns lagUtbetalingsoppdrag(emptyList<Utbetalingsperiode>())

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            saksbehandlerId = "12345",
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke kjøre sammenligning dersom det ikke finnes noen utbetalingsperioder i utbetalingsoppdraget fra ny`() {
        setupMocks()
        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        } returns BeregnetUtbetalingsoppdragLongId(
            lagUtbetalingsoppdrag(emptyList<no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode>()),
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = mockk(),
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom ett av simuleringsresultatene ikke er tomt men det andre er det`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mai(2023), jun(2023), 300),
                Periode(aug(2023), sep(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.DetEneSimuleringsresultatetErTomt)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke kjøre sammenligning dersom begge simuleringsresultatene er tomme`() {
        setupMocks()
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal fange opp feil som kastes`() {
        setupMocks()
        every {
            økonomiKlient.hentSimulering(any())
        } throws Exception("Test")

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = mockk(),
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.UventetFeil)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom antall andeler fra ny generator er ulikt andeler med utbetaling`() {
        setupMocks(
            overstyrteAndelerFraGenerator = listOf(
                AndelMedPeriodeIdLongId(0, 1, null, 1),
                AndelMedPeriodeIdLongId(1, 2, 1, 1),
                AndelMedPeriodeIdLongId(2, 3, 2, 1),
            ),
            overstyrteAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned(),
                    tom = inneværendeMåned().plusMonths(1),
                ),
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned().plusMonths(2),
                    tom = inneværendeMåned().plusMonths(3),
                ),
            ),
        )

        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.FeilAntallAndeler)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom id'ene til andeler fra ny generator ikke matcher id'ene til andeler med utbetaling`() {
        setupMocks(
            overstyrteAndelerFraGenerator = listOf(
                AndelMedPeriodeIdLongId(0, 1, null, 1),
                AndelMedPeriodeIdLongId(1, 2, 1, 1),
                AndelMedPeriodeIdLongId(2, 3, 2, 1),
            ),
            overstyrteAndeler = listOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = inneværendeMåned(),
                    tom = inneværendeMåned().plusMonths(1),
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = inneværendeMåned().plusMonths(2),
                    tom = inneværendeMåned().plusMonths(3),
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = inneværendeMåned().plusMonths(4),
                    tom = inneværendeMåned().plusMonths(5),
                ),
            ),
        )

        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.AndelerMatcherIkke)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom andeler fra ny generator matcher andeler med utbetaling`() {
        setupMocks(
            overstyrteAndelerFraGenerator = listOf(
                AndelMedPeriodeIdLongId(0, 1, null, 1),
                AndelMedPeriodeIdLongId(1, 2, 1, 1),
                AndelMedPeriodeIdLongId(2, 3, 2, 1),
            ),
            overstyrteAndeler = listOf(
                lagAndelTilkjentYtelse(
                    id = 0,
                    fom = inneværendeMåned(),
                    tom = inneværendeMåned().plusMonths(1),
                ),
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = inneværendeMåned().plusMonths(2),
                    tom = inneværendeMåned().plusMonths(3),
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = inneværendeMåned().plusMonths(4),
                    tom = inneværendeMåned().plusMonths(5),
                ),
            ),
        )

        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            emptyList(),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            gammeltSimuleringResultat = simuleringBasertPåGammelGenerator,
            gammeltUtbetalingsoppdrag = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    private fun setupMocks(
        overstyrteAndelerFraGenerator: List<AndelMedPeriodeIdLongId>? = null,
        overstyrteAndeler: List<AndelTilkjentYtelse>? = null,
    ) {
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.KONTROLLER_NY_UTBETALINGSGENERATOR,
                false,
            )
        } returns true

        val beregnetUtbetalingsoppdragMock = mockk<BeregnetUtbetalingsoppdragLongId>()
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()

        // every { utbetalingsoppdrag.kodeEndring } returns Utbetalingsoppdrag.KodeEndring.ENDR
        //
        // every { utbetalingsoppdrag.fagSystem } returns FagsystemBA.BARNETRYGD
        //
        // every { utbetalingsoppdrag.utbetalingsperiode } returns listOf(mockk())

        every { beregnetUtbetalingsoppdragMock.utbetalingsoppdrag } returns utbetalingsoppdrag

        every { beregnetUtbetalingsoppdragMock.andeler } returns (overstyrteAndelerFraGenerator ?: emptyList())

        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        } returns beregnetUtbetalingsoppdragMock

        every { tikjentYtelseRepository.findByBehandling(any()) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.addAll(
                overstyrteAndeler ?: emptyList(),
            )
        }
    }

    fun lagDetaljertSimuleringsResultat(perioder: List<Periode<Int, Måned>>) =
        if (perioder.isEmpty()) {
            DetaljertSimuleringResultat(emptyList())
        } else {
            DetaljertSimuleringResultat(
                simuleringMottaker =
                listOf(
                    SimuleringMottaker(
                        simulertPostering = perioder.fold(mutableListOf()) { acc, periode ->
                            if (periode.innhold!! == 0) {
                                acc.addAll(
                                    listOf(
                                        lagSimulertPostering(periode, overstyrtBeløp = 1000),
                                        lagSimulertPostering(
                                            periode = periode,
                                            overstyrtBeløp = 1000,
                                            negativtFortegn = true,
                                        ),
                                    ),
                                )
                                acc
                            } else {
                                acc.add(
                                    lagSimulertPostering(periode),
                                )
                                acc
                            }
                        },
                        mottakerType = MottakerType.BRUKER,
                    ),
                ),
            )
        }

    fun lagSimulertPostering(
        periode: Periode<Int, Måned>,
        overstyrtBeløp: Int? = null,
        negativtFortegn: Boolean = false,
    ) =
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = periode.fraOgMed.tilLocalDate(),
            tom = periode.tilOgMed.tilLocalDate(),
            betalingType = BetalingType.DEBIT,
            beløp = if (negativtFortegn) {
                -BigDecimal(overstyrtBeløp ?: periode.innhold!!).setScale(10)
            } else {
                BigDecimal(overstyrtBeløp ?: periode.innhold!!).setScale(
                    10,
                )
            },
            posteringType = PosteringType.YTELSE,
            forfallsdato = des(2023).tilLocalDate(),
            utenInntrekk = true,
        )
}
