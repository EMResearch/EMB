package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SmåbarnstilleggBarnetrygdGeneratorTest {

    val søker = lagPerson(type = PersonType.SØKER)
    val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
    val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(2).minusMonths(1))
    val barn3 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(4).minusMonths(1))

    val behandling = lagBehandling()
    val tilkjentYtelse =
        TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal kun få småbarnstillegg når alle tre krav er oppfylt i samme periode`() {
        val overgangsstønadPerioder = listOf(
            InternPeriodeOvergangsstønad(
                personIdent = søker.aktør.aktivFødselsnummer(),
                fomDato = LocalDate.now().minusYears(2),
                tomDato = LocalDate.now(),
            ),
        )

        val utvidetAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(3),
                tom = YearMonth.now().plusYears(1),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
        )

        val barnasAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn3.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().plusYears(2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn3,
            ),
        )

        val småbarnstilleggAndeler =
            SmåbarnstilleggBarnetrygdGenerator(behandlingId = behandling.id, tilkjentYtelse = tilkjentYtelse)
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = overgangsstønadPerioder,
                    utvidetAndeler = utvidetAndeler,
                    barnasAndeler = barnasAndeler,
                    barnasAktørerOgFødselsdatoer = listOf(Pair(barn3.aktør, barn3.fødselsdato)),
                )

        Assertions.assertEquals(1, småbarnstilleggAndeler.size)
        Assertions.assertEquals(YearMonth.now().minusYears(2), småbarnstilleggAndeler.single().stønadFom)
        Assertions.assertEquals(barn3.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndeler.single().stønadTom)
        Assertions.assertEquals(BigDecimal(100), småbarnstilleggAndeler.single().prosent)
    }

    @Test
    fun `Skal lage småbarnstillegg-andeler med 0kr når enten utvidet eller barnet under 3 år er overstyrt til 0kr`() {
        val overgangsstønadPerioder = listOf(
            InternPeriodeOvergangsstønad(
                personIdent = søker.aktør.aktivFødselsnummer(),
                fomDato = LocalDate.now().minusYears(4),
                tomDato = LocalDate.now().plusYears(1),
            ),
        )

        val utvidetAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(4),
                tom = YearMonth.now().minusYears(3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(3).plusMonths(1),
                tom = YearMonth.now().minusYears(2),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
                prosent = BigDecimal.ZERO,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(2).plusMonths(1),
                tom = YearMonth.now(),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
        )

        val barnasAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn3.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().minusYears(3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn3,
                prosent = BigDecimal.ZERO,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(3).plusMonths(1),
                tom = YearMonth.now().plusYears(2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn3,
            ),
        )

        val småbarnstilleggAndeler =
            SmåbarnstilleggBarnetrygdGenerator(behandlingId = behandling.id, tilkjentYtelse = tilkjentYtelse)
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = overgangsstønadPerioder,
                    utvidetAndeler = utvidetAndeler,
                    barnasAndeler = barnasAndeler,
                    barnasAktørerOgFødselsdatoer = listOf(Pair(barn3.aktør, barn3.fødselsdato)),
                )

        Assertions.assertEquals(2, småbarnstilleggAndeler.size)
        Assertions.assertEquals(barn3.fødselsdato.plusMonths(1).toYearMonth(), småbarnstilleggAndeler.first().stønadFom)
        Assertions.assertEquals(YearMonth.now().minusYears(2), småbarnstilleggAndeler.first().stønadTom)
        Assertions.assertEquals(BigDecimal.ZERO, småbarnstilleggAndeler.first().prosent)

        Assertions.assertEquals(YearMonth.now().minusYears(2).plusMonths(1), småbarnstilleggAndeler.last().stønadFom)
        Assertions.assertEquals(barn3.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndeler.last().stønadTom)
        Assertions.assertEquals(BigDecimal(100), småbarnstilleggAndeler.last().prosent)
    }

    @Test
    fun `Skal lage småbarnstillegg-andeler med riktig prosent når vi har to barn hvor 1 av de har nullutbetaling`() {
        val overgangsstønadPerioder = listOf(
            InternPeriodeOvergangsstønad(
                personIdent = søker.aktør.aktivFødselsnummer(),
                fomDato = LocalDate.now().minusYears(3),
                tomDato = LocalDate.now().plusYears(3),
            ),
        )

        val utvidetAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(3),
                tom = YearMonth.now().plusYears(2),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
        )

        val barnasAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn1.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().minusYears(1),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn1,
                prosent = BigDecimal.ZERO,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(1).plusMonths(1),
                tom = YearMonth.now().plusYears(5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn1,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn2.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().plusYears(6),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn2,
            ),
        )

        val småbarnstilleggAndeler =
            SmåbarnstilleggBarnetrygdGenerator(behandlingId = behandling.id, tilkjentYtelse = tilkjentYtelse)
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = overgangsstønadPerioder,
                    utvidetAndeler = utvidetAndeler,
                    barnasAndeler = barnasAndeler,
                    barnasAktørerOgFødselsdatoer = listOf(
                        Pair(barn1.aktør, barn1.fødselsdato),
                        Pair(barn2.aktør, barn2.fødselsdato),
                    ),
                )

        Assertions.assertEquals(2, småbarnstilleggAndeler.size)
        Assertions.assertEquals(barn1.fødselsdato.plusMonths(1).toYearMonth(), småbarnstilleggAndeler.first().stønadFom)
        Assertions.assertEquals(barn2.fødselsdato.toYearMonth(), småbarnstilleggAndeler.first().stønadTom)
        Assertions.assertEquals(BigDecimal.ZERO, småbarnstilleggAndeler.first().prosent)

        Assertions.assertEquals(barn2.fødselsdato.plusMonths(1).toYearMonth(), småbarnstilleggAndeler.last().stønadFom)
        Assertions.assertEquals(barn2.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndeler.last().stønadTom)
        Assertions.assertEquals(BigDecimal(100), småbarnstilleggAndeler.last().prosent)
    }

    @Test
    fun `Skal lage småbarnstillegg-andeler med 0kr for 2 barn når søker sin utvidet del er overstyrt til 0kr`() {
        val overgangsstønadPerioder = listOf(
            InternPeriodeOvergangsstønad(
                personIdent = søker.aktør.aktivFødselsnummer(),
                fomDato = LocalDate.now().minusYears(3),
                tomDato = LocalDate.now().plusYears(3),
            ),
        )

        val utvidetAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn1.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().minusYears(1),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
                prosent = BigDecimal.ZERO,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.now().minusYears(1).plusMonths(1),
                tom = YearMonth.now().plusYears(2),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
        )

        val barnasAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn1.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().plusYears(5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn1,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = barn2.fødselsdato.toYearMonth().plusMonths(1),
                tom = YearMonth.now().plusYears(6),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn2,
            ),
        )

        val småbarnstilleggAndeler =
            SmåbarnstilleggBarnetrygdGenerator(behandlingId = behandling.id, tilkjentYtelse = tilkjentYtelse)
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = overgangsstønadPerioder,
                    utvidetAndeler = utvidetAndeler,
                    barnasAndeler = barnasAndeler,
                    barnasAktørerOgFødselsdatoer = listOf(
                        Pair(barn1.aktør, barn1.fødselsdato),
                        Pair(barn2.aktør, barn2.fødselsdato),
                    ),
                )

        Assertions.assertEquals(2, småbarnstilleggAndeler.size)
        Assertions.assertEquals(barn1.fødselsdato.plusMonths(1).toYearMonth(), småbarnstilleggAndeler.first().stønadFom)
        Assertions.assertEquals(YearMonth.now().minusYears(1), småbarnstilleggAndeler.first().stønadTom)
        Assertions.assertEquals(BigDecimal.ZERO, småbarnstilleggAndeler.first().prosent)

        Assertions.assertEquals(YearMonth.now().minusYears(1).plusMonths(1), småbarnstilleggAndeler.last().stønadFom)
        Assertions.assertEquals(barn2.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndeler.last().stønadTom)
        Assertions.assertEquals(BigDecimal(100), småbarnstilleggAndeler.last().prosent)
    }
}
