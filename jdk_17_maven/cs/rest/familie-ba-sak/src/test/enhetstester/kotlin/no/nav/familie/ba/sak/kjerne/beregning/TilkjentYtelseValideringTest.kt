package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseValideringTest {

    val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDate.now())

    @Test
    fun `Skal returnere true når person har etterbetaling som er mer enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val andeler1 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )
        val forrigeAndeler1 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler1,
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler1,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )

        val forrigeAndeler2 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2,
            ),
        )
        val andeler2 = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler2,
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertTrue(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler2,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved uendret tilkjent ytelse andel mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = andeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved reduksjon av beløp mer enn 3 år tilbake`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person1,
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = emptyList(),
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            ),
        )
    }

    @Test
    fun `Skal returnere false ved endring av tilkjent ytelse andel som er mindre enn 3 år tilbake i tid`() {
        val person1 = tilfeldigPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1,
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDate.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDate.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = emptyList(),
                andelerForPerson = andeler,
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDate.now().minusYears(2)),
            ),
        )

        Assertions.assertFalse(
            TilkjentYtelseValidering.erUgyldigEtterbetalingPåPerson(
                forrigeAndelerForPerson = forrigeAndeler,
                andelerForPerson = emptyList(),
                gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(LocalDate.now().minusYears(2)),
            ),
        )
    }

    @Test
    fun `Skal finne overlappende perioder og returnere periode med første fom og siste tom`() {
        val barn = tilfeldigPerson()
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().plusMonths(1),
                tom = inneværendeMåned().plusYears(1),
                beløp = 2108,
                person = barn,
            ),
        )
        val andelerFraTidligere = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).plusMonths(1),
                tom = inneværendeMåned().plusMonths(2),
                beløp = 2108,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().plusMonths(3),
                tom = inneværendeMåned().plusYears(1).minusMonths(1),
                beløp = 2108,
                person = barn,
            ),
        )

        Assertions.assertEquals(
            listOf(
                MånedPeriode(
                    inneværendeMåned().minusYears(1).plusMonths(1),
                    inneværendeMåned().plusYears(1).minusMonths(1),
                ),
            ),
            TilkjentYtelseValidering.finnPeriodeMedOverlappAvAndeler(andeler, andelerFraTidligere),
        )
        Assertions.assertEquals(
            TilkjentYtelseValidering.finnPeriodeMedOverlappAvAndeler(andeler, emptyList()),
            emptyList<MånedPeriode>(),
        )
    }

    @Test
    fun `Skal håndtere overlappende tidligere andeler fra flere enn 1 behandling`() {
        val barn = tilfeldigPerson()
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().plusMonths(1),
                tom = inneværendeMåned().plusYears(1),
                beløp = 2108,
                person = barn,
            ),
        )

        // 3 Behandlinger med identiske perioder.
        val andelerFraTidligere = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).plusMonths(1),
                tom = inneværendeMåned().plusMonths(2),
                beløp = 2108,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).plusMonths(1),
                tom = inneværendeMåned().plusMonths(2),
                beløp = 2108,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).plusMonths(1),
                tom = inneværendeMåned().plusMonths(2),
                beløp = 2108,
                person = barn,
            ),
        )

        Assertions.assertEquals(
            listOf(
                MånedPeriode(
                    inneværendeMåned().minusYears(1).plusMonths(1),
                    inneværendeMåned().plusMonths(2),
                ),
            ),
            TilkjentYtelseValidering.finnPeriodeMedOverlappAvAndeler(andeler, andelerFraTidligere),
        )
        Assertions.assertEquals(
            TilkjentYtelseValidering.finnPeriodeMedOverlappAvAndeler(andeler, emptyList()),
            emptyList<MånedPeriode>(),
        )
    }

    @Nested
    inner class `Valider at satsendring kun oppdaterer sats på eksisterende perioder` {
        @Test
        fun `Skal kaste feil hvis person har lagt til andel som ikke hadde utbetaling i forrige behandling`() {
            val person = lagPerson(type = PersonType.BARN)
            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1083,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            assertThatThrownBy {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Satsendring kan ikke legge til en andel som ikke var der i forrige behandling")
        }

        @Test
        fun `Skal kaste feil hvis person har fått fjernet andel i periode som hadde utbetaling før`() {
            val person = lagPerson(type = PersonType.BARN)
            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            assertThatThrownBy {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Satsendring kan ikke fjerne en andel som fantes i forrige behandling")
        }

        @Test
        fun `Skal kaste feil hvis person har fått endret prosent på andel`() {
            val person = lagPerson(type = PersonType.BARN)
            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 527,
                    prosent = BigDecimal(50),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            assertThatThrownBy {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Satsendring kan ikke endre på prosenten til en andel")
        }

        @Test
        fun `Skal ikke kaste feil hvis det eneste som er gjort er å oppdatere sats`() {
            val person = lagPerson(type = PersonType.BARN)
            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1083,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            assertDoesNotThrow {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }
        }

        @Test
        fun `Skal kaste feil hvis det ikke eksisterte andeler forrige gang men gjør det nå`() {
            val person = lagPerson(type = PersonType.BARN)
            val forrigeAndeler = emptyList<AndelTilkjentYtelse>()

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1083,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            assertThatThrownBy {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Satsendring kan ikke legge til en andel som ikke var der i forrige behandling")
        }

        @Test
        fun `Skal kaste feil hvis det eksisterte andeler forrige gang men ikke gjør det nå`() {
            val person = lagPerson(type = PersonType.BARN)

            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 5),
                    beløp = 1083,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = person.aktør,
                ),
            )

            val nåværendeAndeler = emptyList<AndelTilkjentYtelse>()

            assertThatThrownBy {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Satsendring kan ikke fjerne en andel som fantes i forrige behandling")
        }

        @Test
        fun `Skal kaste feil hvis det eksisterer andeler i lik periode som forrige gang men av ulik type`() {
            val barn = lagPerson(type = PersonType.BARN)

            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = barn.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD, // barn kan ha utvidet på enslig mindreårig-saker
                    aktør = barn.aktør,
                ),
            )

            assertThrows<Feil> {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }
        }

        @Test
        fun `Skal kaste feil hvis det eksisterer andeler i lik periode som forrige gang men på forskjellige personer`() {
            val barn1 = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)

            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = barn1.aktør,
                ),
            )

            val nåværendeAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    beløp = 1054,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    aktør = barn2.aktør,
                ),
            )

            assertThrows<Feil> {
                TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                    andelerFraForrigeBehandling = forrigeAndeler,
                    andelerTilkjentYtelse = nåværendeAndeler,
                )
            }
        }
    }
}
