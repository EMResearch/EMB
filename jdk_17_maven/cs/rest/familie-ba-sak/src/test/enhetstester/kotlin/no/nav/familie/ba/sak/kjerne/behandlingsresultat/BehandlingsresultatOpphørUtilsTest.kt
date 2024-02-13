package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth

class BehandlingsresultatOpphørUtilsTest {

    val søker = tilfeldigPerson()

    val jan22 = YearMonth.of(2022, 1)
    val feb22 = YearMonth.of(2022, 2)
    val mar22 = YearMonth.of(2022, 3)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler strekker seg lengre enn dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns YearMonth.of(2022, 4)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører mens forrige andeler ikke opphører til og med dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns YearMonth.of(2022, 4)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører tidligere enn forrige andeler og dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom vi går fra andeler på person til fullt opphør på person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = emptyList(),
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler men det er i fremtiden`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList(),
        )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ALLEREDE_UTBETALT", "ENDRE_MOTTAKER", "ETTERBETALING_3ÅR"])
    internal fun `filtrerBortIrrelevanteAndeler - skal filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR`(årsak: Årsak) {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 0,
                aktør = barnAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = mar22,
                tom = mai22,
                beløp = 1400,
                aktør = barnAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = aug22,
                tom = aug22,
                beløp = 0,
                aktør = barnAktør,
            ),
        )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = jan22,
                    tom = feb22,
                    årsak = årsak,
                ),
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = aug22,
                    tom = aug22,
                    årsak = årsak,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, mar22)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, mai22)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak DELT_BOSTED`() {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 0,
                aktør = barnAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = mar22,
                tom = mai22,
                beløp = 1400,
                aktør = barnAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = aug22,
                tom = aug22,
                beløp = 0,
                aktør = barnAktør,
            ),
        )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = jan22,
                    tom = feb22,
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = aug22,
                    tom = aug22,
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, jan22)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, aug22)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp grunnet differanseberegning`() {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør
        val søker = lagPerson(type = PersonType.SØKER)
        val søkerAktør = søker.aktør

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 0,
                differanseberegnetPeriodebeløp = 50,
                aktør = søkerAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = mar22,
                tom = mai22,
                beløp = 0,
                differanseberegnetPeriodebeløp = 50,
                aktør = barnAktør,
            ),
            lagAndelTilkjentYtelse(
                fom = aug22,
                tom = aug22,
                beløp = 0,
                differanseberegnetPeriodebeløp = 50,
                aktør = barnAktør,
            ),
        )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretAndeler = emptyList())

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, jan22)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, aug22)
    }
}
