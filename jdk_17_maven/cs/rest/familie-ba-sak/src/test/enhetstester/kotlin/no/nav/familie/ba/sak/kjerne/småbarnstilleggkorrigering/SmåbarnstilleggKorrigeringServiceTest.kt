package no.nav.familie.ba.sak.kjerne.småbarnstilleggkorrigering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class SmåbarnstilleggKorrigeringServiceTest {

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK(relaxed = true)
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var småbarnstilleggKorrigeringService: SmåbarnstilleggKorrigeringService

    @Test
    fun `leggTilSmåbarnstilleggPåBehandling skal legge til småbarnstillegg på behandling som en AndelTilkjentYtelse`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)

        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns tilkjentYtelse

        val småbarnsTillegg =
            småbarnstilleggKorrigeringService.leggTilSmåbarnstilleggPåBehandling(YearMonth.of(2020, 10), behandling)

        verify(exactly = 1) { tilkjentYtelseRepository.findByBehandling(behandling.id) }
        verify(exactly = 1) {
            loggService.opprettSmåbarnstilleggLogg(
                behandling,
                "Småbarnstillegg for oktober 2020 lagt til",
            )
        }

        assertThat(småbarnsTillegg.size, Is(1))
        assertThat(småbarnsTillegg[0].type, Is(YtelseType.SMÅBARNSTILLEGG))
        assertThat(småbarnsTillegg[0].stønadFom, Is(YearMonth.of(2020, 10)))
        assertThat(småbarnsTillegg[0].stønadTom, Is(YearMonth.of(2020, 10)))
    }

    @Test
    fun `leggTilSmåbarnstilleggPåBehandling skal kaste feil hvis småbarnstillegg allerede finnes for periode`() {
        val behandling = lagBehandling()
        val tilkjentYtelseMock = mockk<TilkjentYtelse>()

        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            fom = YearMonth.of(2010, 10),
            tom = YearMonth.of(2020, 10),
            ytelseType = YtelseType.SMÅBARNSTILLEGG,
        )

        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns tilkjentYtelseMock
        every { tilkjentYtelseMock.andelerTilkjentYtelse } returns mutableSetOf(andelTilkjentYtelse)

        val feil = assertThrows<FunksjonellFeil> {
            småbarnstilleggKorrigeringService.leggTilSmåbarnstilleggPåBehandling(YearMonth.of(2020, 10), behandling)
        }

        assertThat(
            feil.melding,
            Is("Det er ikke mulig å legge til småbarnstillegg for oktober 2020 fordi det allerede finnes småbarnstillegg for denne perioden"),
        )
    }

    @Test
    fun `fjernSmåbarnstilleggPåBehandling skal splitte eksisterende overlappende småbarnstilleggsperiode`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)

        tilkjentYtelse.andelerTilkjentYtelse.add(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2010, 10),
                tom = YearMonth.of(2020, 10),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
            ),
        )

        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns tilkjentYtelse
        every { tilkjentYtelseRepository.saveAndFlush(any()) } returns tilkjentYtelse

        val oppsplittetSmåbarnstillegg =
            småbarnstilleggKorrigeringService.fjernSmåbarnstilleggPåBehandling(YearMonth.of(2020, 5), behandling)

        verify(exactly = 1) {
            loggService.opprettSmåbarnstilleggLogg(
                behandling,
                "Småbarnstillegg for mai 2020 fjernet",
            )
        }

        assertThat(oppsplittetSmåbarnstillegg.size, Is(2))

        assertThat(oppsplittetSmåbarnstillegg[0].stønadFom, Is(YearMonth.of(2010, 10)))
        assertThat(oppsplittetSmåbarnstillegg[0].stønadTom, Is(YearMonth.of(2020, 4)))

        assertThat(oppsplittetSmåbarnstillegg[1].stønadFom, Is(YearMonth.of(2020, 6)))
        assertThat(oppsplittetSmåbarnstillegg[1].stønadTom, Is(YearMonth.of(2020, 10)))
    }

    @Test
    fun `fjernSmåbarnstilleggPåBehandling skal kaste feil hvis småbarnstillegg ikke finnes for periode`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)

        tilkjentYtelse.andelerTilkjentYtelse.add(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2010, 10),
                tom = YearMonth.of(2020, 10),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
            ),
        )

        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns tilkjentYtelse

        val feil = assertThrows<FunksjonellFeil> {
            småbarnstilleggKorrigeringService.fjernSmåbarnstilleggPåBehandling(YearMonth.of(2025, 5), behandling)
        }

        assertThat(
            feil.melding,
            Is("Det er ikke mulig å fjerne småbarnstillegg for mai 2025 fordi det ikke finnes småbarnstillegg for denne perioden"),
        )
    }
}
