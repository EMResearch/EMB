package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class UtbetalingsoppdragGeneratorServiceTest {

    @MockK
    private lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var unleashService: UnleashService

    @InjectMockKs
    private lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator

    @InjectMockKs
    private lateinit var utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset når det ikke finnes en forrige behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 3,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets = listOf(
                Pair(0L, null),
                Pair(1L, 0L),
                Pair(2L, 1L),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset når det finnes en forrige behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 300,
                    person = person,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0,
                    forrigeperiodeIdOffset = null,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2,
                    forrigeperiodeIdOffset = 1,
                ),
            ),
        )

        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 1,
            forventetAntallUtbetalingsperioder = 1,
            forventedeOffsets = listOf(
                Pair(3L, 2L),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset for 2 personer`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 5,
            forventetAntallUtbetalingsperioder = 5,
            forventedeOffsets = listOf(
                Pair(0L, null),
                Pair(1L, 0L),
                Pair(2L, 1L),
                Pair(3L, null),
                Pair(4L, 3L),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset for 2 personer og tidligere behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2L,
                    forrigeperiodeIdOffset = 1L,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = barn,
                    periodeIdOffset = 3L,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                    periodeIdOffset = 4L,
                    forrigeperiodeIdOffset = 3L,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 2,
            forventedeOffsets = listOf(
                Pair(5L, 2L),
                Pair(6L, 4L),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag med endret migreringsdato for en eksisterende kjede og en ny kjede`() {
        val vedtak = lagVedtak(behandling = lagBehandling(behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2L,
                    forrigeperiodeIdOffset = 1L,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            migreringsdato = YearMonth.of(2022, 11).førsteDagIInneværendeMåned(),
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets = listOf(
                Pair(3L, 2L),
                Pair(4L, null),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag for simulering med en eksisterende kjede og en ny kjede`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2L,
                    forrigeperiodeIdOffset = 1L,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = "abc123",
                erSimulering = true,
            )

        verify(exactly = 0) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = emptySet(),
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets = listOf(
                Pair(3L, 2L),
                Pair(4L, null),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - revurdering hvor 0-utbetaling går til betaling skal ikke opprette noe opphør ved simulering`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 0,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 0,
                    person = barn,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = "abc123",
                erSimulering = true,
            )

        verify(exactly = 0) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = emptySet(),
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 2,
            forventedeOffsets = listOf(
                Pair(0L, null),
                Pair(1L, null),
            ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag men ikke oppdatere andeler med offset når toggel er av`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            brukNyUtbetalingsgeneratorToggleErPå = false,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        verify(exactly = 0) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
            forventetAntallAndeler = 3,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets = listOf(
                Pair(null, null),
                Pair(null, null),
                Pair(null, null),
            ),
        )
    }

    private fun validerBeregnetUtbetalingsoppdragOgAndeler(
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
        andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
        forventedeOffsets: List<Pair<Long?, Long?>>,
        forventetAntallAndeler: Int,
        forventetAntallUtbetalingsperioder: Int,
    ) {
        assertThat(beregnetUtbetalingsoppdrag.utbetalingsoppdrag).isNotNull
        assertThat(beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode.size).isEqualTo(
            forventetAntallUtbetalingsperioder,
        )

        assertThat(beregnetUtbetalingsoppdrag.andeler).isNotEmpty
        assertThat(beregnetUtbetalingsoppdrag.andeler.size).isEqualTo(forventetAntallAndeler)

        if (andelerTilkjentYtelse.isNotEmpty()) {
            assertThat(andelerTilkjentYtelse.size).isEqualTo(forventetAntallAndeler)
            assertThat(
                andelerTilkjentYtelse.map {
                    Pair(
                        it.periodeOffset,
                        it.forrigePeriodeOffset,
                    )
                },
            ).isEqualTo(
                forventedeOffsets,
            )
        } else {
            assertThat(
                beregnetUtbetalingsoppdrag.andeler.map {
                    Pair(
                        it.periodeId,
                        it.forrigePeriodeId,
                    )
                },
            ).isEqualTo(
                forventedeOffsets,
            )
        }
    }

    private fun setUpMocks(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        tilkjentYtelseSlot: CapturingSlot<TilkjentYtelse>,
        brukNyUtbetalingsgeneratorToggleErPå: Boolean = true,
        forrigeTilkjentYtelse: TilkjentYtelse? = null,
        migreringsdato: LocalDate? = null,
    ) {
        if (forrigeTilkjentYtelse == null) {
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns null
            every { andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(behandling.fagsak.id) } returns emptyList()
        } else {
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) } returns forrigeTilkjentYtelse.behandling

            every { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(forrigeTilkjentYtelse.behandling.id) } returns forrigeTilkjentYtelse

            every { andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(behandling.fagsak.id) } returns
                forrigeTilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
                    .groupBy { it.aktør.aktivFødselsnummer() }
                    .mapValues { it.value.maxBy { it.periodeOffset!! } }.values.toList()
        }

        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns tilkjentYtelse

        every { behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id) } returns listOf(behandling)

        every { behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id) } returns migreringsdato

        every {
            unleashService.isEnabled(
                toggleId = FeatureToggleConfig.BRUK_NY_UTBETALINGSGENERATOR,
                properties = any(),
            )
        } returns brukNyUtbetalingsgeneratorToggleErPå

        every { tilkjentYtelseRepository.save(capture(tilkjentYtelseSlot)) } returns mockk()
    }
}
