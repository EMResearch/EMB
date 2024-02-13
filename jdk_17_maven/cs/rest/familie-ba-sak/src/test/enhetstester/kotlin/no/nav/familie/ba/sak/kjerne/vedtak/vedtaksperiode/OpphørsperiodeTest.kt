package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class OpphørsperiodeTest {

    val januar2023 = YearMonth.of(2023, 1)
    val februar2023 = YearMonth.of(2023, 2)
    val mars2023 = YearMonth.of(2023, 3)
    val april2023 = YearMonth.of(2023, 4)
    val mai2023 = YearMonth.of(2023, 5)
    val mai2024 = YearMonth.of(2024, 5)

    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    val personopplysningGrunnlag = PersonopplysningGrunnlag(
        behandlingId = 0L,
        personer = mutableSetOf(søker, barn1, barn2),
    )

    @Test
    fun `Skal utlede opphørsperiode mellom oppfylte perioder`() {
        val periodeTomFørsteAndel = inneværendeMåned().minusYears(2)
        val periodeFomAndreAndel = inneværendeMåned().minusYears(1)
        val periodeTomAndreAndel = inneværendeMåned().minusMonths(10)
        val periodeFomSisteAndel = inneværendeMåned().minusMonths(4)
        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(4),
            periodeTomFørsteAndel,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val andel2Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            periodeFomAndreAndel,
            periodeTomAndreAndel,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val andel3Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            periodeFomSisteAndel,
            inneværendeMåned().plusMonths(12),
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            andelerTilkjentYtelse = listOf(andelBarn1, andel2Barn1, andel3Barn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(2, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(periodeFomAndreAndel.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())

        assertEquals(periodeTomAndreAndel.nesteMåned(), opphørsperioder[1].periodeFom.toYearMonth())
        assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[1].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal utlede opphørsperiode når siste utbetalingsperiode er før neste måned`() {
        val periodeTomFørsteAndel = inneværendeMåned().minusYears(1)
        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(4),
            periodeTomFørsteAndel,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            andelerTilkjentYtelse = listOf(andelBarn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `Skal utlede opphørsperiode fra neste måned når siste utbetalingsperiode er inneværende måned`() {
        val periodeTomFørsteAndel = inneværendeMåned()
        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(4),
            periodeTomFørsteAndel,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            andelerTilkjentYtelse = listOf(andelBarn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `Skal utlede opphørsperiode når ytelsen reduseres i revurdering`() {
        val reduksjonFom = inneværendeMåned().minusYears(5)
        val reduksjonTom = inneværendeMåned().minusYears(3)
        val forrigeAndelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(5),
            inneværendeMåned().plusMonths(12),
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            reduksjonTom,
            inneværendeMåned().plusMonths(12),
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1),
            andelerTilkjentYtelse = listOf(andelBarn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
            forrigePersonopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(reduksjonFom, opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(reduksjonTom.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal utlede opphørsperiode når ytelsen reduseres i revurdering og to inntilliggende perioder opphøres`() {
        val forrigeAndel1Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            januar2023,
            februar2023,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val forrigeAndel2Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            mars2023,
            april2023,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            mai2023,
            mai2024,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndel1Barn1, forrigeAndel2Barn1),
            andelerTilkjentYtelse = listOf(andelBarn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
            forrigePersonopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(januar2023, opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(april2023, opphørsperioder[0].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal utlede opphørsperiode når ytelsen reduseres i revurdering og ytelsen ikke lenger er løpende`() {
        val reduksjonFom = inneværendeMåned()
        val forrigeAndelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(5),
            inneværendeMåned().plusMonths(12),
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val andelBarn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            inneværendeMåned().minusYears(5),
            reduksjonFom,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1),
            andelerTilkjentYtelse = listOf(andelBarn1),
            personopplysningGrunnlag = personopplysningGrunnlag,
            forrigePersonopplysningGrunnlag = personopplysningGrunnlag,
        ).run(::slåSammenOpphørsperioder)

        assertEquals(1, opphørsperioder.size)
        assertEquals(reduksjonFom.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal slå sammen to like opphørsperioder`() {
        val periode12MånederFraInneværendeMåned = inneværendeMåned().minusMonths(12).toLocalDate()

        val toLikePerioder = listOf(
            Opphørsperiode(
                periodeFom = periode12MånederFraInneværendeMåned,
                periodeTom = inneværendeMåned().toLocalDate(),
            ),
            Opphørsperiode(
                periodeFom = periode12MånederFraInneværendeMåned,
                periodeTom = inneværendeMåned().toLocalDate(),
            ),
        )

        assertEquals(1, slåSammenOpphørsperioder(toLikePerioder).size)
    }

    @Test
    fun `Skal slå sammen to opphørsperioder med ulik sluttdato`() {
        val toPerioderMedUlikSluttdato = listOf(
            Opphørsperiode(
                periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                periodeTom = inneværendeMåned().toLocalDate(),
            ),
            Opphørsperiode(
                periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                periodeTom = inneværendeMåned().nesteMåned().toLocalDate(),
            ),
        )
        val enPeriodeMedSluttDatoNesteMåned = slåSammenOpphørsperioder(toPerioderMedUlikSluttdato)

        assertEquals(1, enPeriodeMedSluttDatoNesteMåned.size)
        assertEquals(inneværendeMåned().nesteMåned().toLocalDate(), enPeriodeMedSluttDatoNesteMåned.first().periodeTom)
    }

    @Test
    fun `Skal slå sammen to opphørsperioder med ulik startdato`() {
        val toPerioderMedUlikStartdato = listOf(
            Opphørsperiode(
                periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                periodeTom = inneværendeMåned().toLocalDate(),
            ),
            Opphørsperiode(
                periodeFom = inneværendeMåned().minusMonths(13).toLocalDate(),
                periodeTom = inneværendeMåned().toLocalDate(),
            ),
        )
        val enPeriodeMedStartDato13MånederTilbake = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

        assertEquals(1, enPeriodeMedStartDato13MånederTilbake.size)
        assertEquals(
            inneværendeMåned().minusMonths(13).toLocalDate(),
            enPeriodeMedStartDato13MånederTilbake.first().periodeFom,
        )
    }

    @Test
    fun `Skal slå sammen to opphørsperioder som overlapper`() {
        val førsteOpphørsperiodeFom = inneværendeMåned().minusMonths(12).toLocalDate()
        val sisteOpphørsperiodeTom = inneværendeMåned().plusMonths(1).toLocalDate()
        val toPerioderMedUlikStartdato = listOf(
            Opphørsperiode(
                periodeFom = førsteOpphørsperiodeFom,
                periodeTom = inneværendeMåned().minusMonths(2).toLocalDate(),
            ),
            Opphørsperiode(
                periodeFom = inneværendeMåned().minusMonths(6).toLocalDate(),
                periodeTom = sisteOpphørsperiodeTom,
            ),
        )
        val enOpphørsperiodeMedFørsteFomOgSisteTom = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

        assertEquals(1, enOpphørsperiodeMedFørsteFomOgSisteTom.size)
        assertEquals(førsteOpphørsperiodeFom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeFom)
        assertEquals(sisteOpphørsperiodeTom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeTom)
    }

    @Test
    fun `Skal håndtere at det ikke er noen andeler i denne behandlingen`() {
        val forrigeAndel1Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            januar2023,
            februar2023,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val forrigeAndel2Barn1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            mars2023,
            april2023,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            person = barn1,
        )

        val opphørsperioder = mapTilOpphørsperioder(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndel1Barn1, forrigeAndel2Barn1),
            andelerTilkjentYtelse = emptyList(),
            personopplysningGrunnlag = personopplysningGrunnlag,
            forrigePersonopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(januar2023, opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(april2023, opphørsperioder[0].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal håndtere at det ikke er noen andeler i denne eller forrige behandling`() {
        val opphørsperioder = mapTilOpphørsperioder(
            forrigeAndelerTilkjentYtelse = emptyList(),
            andelerTilkjentYtelse = emptyList(),
            personopplysningGrunnlag = personopplysningGrunnlag,
            forrigePersonopplysningGrunnlag = personopplysningGrunnlag,
        )

        assertEquals(0, opphørsperioder.size)
    }
}
