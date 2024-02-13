package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.simulering.KontrollerNyUtbetalingsgeneratorService
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.unleash.UnleashService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class ØkonomiServiceTest {
    @MockK
    private lateinit var økonomiKlient: ØkonomiKlient

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK
    private lateinit var kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService

    @MockK
    private lateinit var utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService

    @MockK
    private lateinit var unleashService: UnleashService

    @InjectMockKs
    private lateinit var økonomiService: ØkonomiService

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal bruke gammel utbetalingsgenerator når toggel er av`() {
        setupMocks(toggelPå = false)

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            lagVedtak(),
            "123abc",
            AndelTilkjentYtelseForIverksettingFactory(),
        )

        verify(exactly = 1) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
            )
        }
        verify(exactly = 1) { beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 0) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        }
    }

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal bruke ny utbetalingsgenerator når toggel er på`() {
        setupMocks(toggelPå = true)

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            lagVedtak(),
            "123abc",
            AndelTilkjentYtelseForIverksettingFactory(),
        )

        verify(exactly = 0) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
            )
        }
        verify(exactly = 0) { beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(any(), any()) }
        verify(exactly = 1) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        }
    }

    private fun setupMocks(toggelPå: Boolean) {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(
            listOf(
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    opphør = null,
                    periodeId = 1,
                    forrigePeriodeId = null,
                    datoForVedtak = LocalDate.now(),
                    klassifisering = "BATR",
                    vedtakdatoFom = inneværendeMåned().førsteDagIInneværendeMåned(),
                    vedtakdatoTom = inneværendeMåned().sisteDagIInneværendeMåned(),
                    sats = BigDecimal(1054),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = "13455678910",
                    behandlingId = 1,
                ),
            ),
        )
        every {
            kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
                any(),
                any(),
            )
        } returns emptyList()
        every {
            unleashService.isEnabled(
                toggleId = FeatureToggleConfig.BRUK_NY_UTBETALINGSGENERATOR,
                properties = any(),
            )
        } returns toggelPå

        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
            )
        } returns utbetalingsoppdrag.tilRestUtbetalingsoppdrag()
        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        } returns BeregnetUtbetalingsoppdragLongId(utbetalingsoppdrag = utbetalingsoppdrag, andeler = emptyList())
        every {
            beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                any(),
                any(),
            )
        } returns mockk()
        every { tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(any()) } just runs
        every { økonomiKlient.iverksettOppdrag(any()) } returns ""
    }
}
