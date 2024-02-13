package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseUtilsEndretUtbetalingAndelTest {

    val behandling = lagBehandling()
    val tilkjentYtelse =
        TilkjentYtelse(behandling = behandling, endretDato = LocalDate.now(), opprettetDato = LocalDate.now())
    val beløp = BigDecimal(100)

    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `teste nye andeler tilkjent ytelse for to barn med endrete utbetalingsandeler`() {
        /**
         * Tidslinjer barn 1:
         * -------------[############]-----------[#########]---------- AndelTilkjentYtelse
         *            0118        0418         1018      0821
         * ---[################]-------------------------------------- EndretUtbetalingYtelse
         *  0115             0318
         *
         * -------------[######][##]-------------[#########]---------- Nye AndelTilkjentYtelse
         *
         * Periodene for nye AndelTilkjentYtelse: 0118-0318, 0418-0418, 1018-0821
         *
         *
         * Tidslinjer barn 2:
         * --------------[###################]--------[###########]------------ AndelTilkjentYtelse
         *              0218               0818     1118        0921
         * ---------------------[####]----[#######################]---[####]--- EndretUtbetalingYtelse
         *                    0418 0518  0718                   0921 1121-1221
         *
         * --------------[#####][####][##][##]--------[###########]------------ Nye AndelTilkjentYtelse
         *
         * Periodene for nye AndelTilkjentYtelse: 0218-0318, 0418-0518, 0618-0618, 0718-0818, 1118-0921
         */

        val andelTilkjentytelseForBarn1 = listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 4)),
            MånedPeriode(YearMonth.of(2018, 10), YearMonth.of(2021, 8)),
        )
            .map {
                lagAndelTilkjentYtelse(barn1, it.fom, it.tom)
            }

        val andelTilkjentytelseForBarn2 = listOf(
            MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 8)),
            MånedPeriode(YearMonth.of(2018, 11), YearMonth.of(2021, 9)),
        )
            .map {
                lagAndelTilkjentYtelse(barn2, it.fom, it.tom)
            }

        val endretUtbetalingerForBarn1 = listOf(
            MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3)),
            MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 4)),
        )
            .map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(behandling.id, barn1, it.fom, it.tom, 50)
            }

        val endretUtbetalingerForBarn2 = listOf(
            MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 5)),
            MånedPeriode(YearMonth.of(2018, 7), YearMonth.of(2021, 9)),
            MånedPeriode(YearMonth.of(2021, 11), YearMonth.of(2021, 12)),
        )
            .map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(behandling.id, barn2, it.fom, it.tom, 50)
            }

        val andelerTilkjentYtelserEtterEUA =
            TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
                (andelTilkjentytelseForBarn1 + andelTilkjentytelseForBarn2),
                endretUtbetalingerForBarn1 + endretUtbetalingerForBarn2,
            )

        val andelerTilkjentYtelserEtterEUAList = andelerTilkjentYtelserEtterEUA.map { it.andel }.toList()

        assertEquals(8, andelerTilkjentYtelserEtterEUAList.size)

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[0],
            barn1.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 1),
            YearMonth.of(2018, 3),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[1],
            barn1.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 4),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[2],
            barn1.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 10),
            YearMonth.of(2021, 8),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[3],
            barn2.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 2),
            YearMonth.of(2018, 3),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[4],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 4),
            YearMonth.of(2018, 5),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[5],
            barn2.aktør.aktivFødselsnummer(),
            beløp,
            YearMonth.of(2018, 6),
            YearMonth.of(2018, 6),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[6],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 7),
            YearMonth.of(2018, 8),
        )

        verifiserAndelTilkjentYtelse(
            andelerTilkjentYtelserEtterEUAList[7],
            barn2.aktør.aktivFødselsnummer(),
            beløp / BigDecimal(2),
            YearMonth.of(2018, 11),
            YearMonth.of(2021, 9),
        )
    }

    @Test
    fun `En gitt MånedPeriode skal gi tilbake perioder med og uten overlapp den har mot en eller flere andre perioder`() {
        val periode = MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 12))

        var perioder = periode.perioderMedOgUtenOverlapp(emptyList())
        var perioderMedOverlapp = perioder.first
        var perioderUtenOverlapp = perioder.second
        assertEquals(0, perioderMedOverlapp.size)
        assertEquals(1, perioderUtenOverlapp.size)
        assertEquals(periode, perioderUtenOverlapp[0])

        perioder = periode.perioderMedOgUtenOverlapp(
            listOf(
                MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2016, 3)),
            ),
        )
        perioderMedOverlapp = perioder.first
        perioderUtenOverlapp = perioder.second
        assertEquals(0, perioderMedOverlapp.size)
        assertEquals(1, perioderUtenOverlapp.size)
        assertEquals(periode, perioderUtenOverlapp[0])

        perioder = periode.perioderMedOgUtenOverlapp(
            listOf(
                periode,
            ),
        )
        perioderMedOverlapp = perioder.first
        perioderUtenOverlapp = perioder.second
        assertEquals(1, perioderMedOverlapp.size)
        assertEquals(periode, perioderMedOverlapp[0])
        assertEquals(0, perioderUtenOverlapp.size)

        perioder = periode.perioderMedOgUtenOverlapp(
            listOf(
                MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3)),
            ),
        )
        perioderMedOverlapp = perioder.first
        perioderUtenOverlapp = perioder.second
        assertEquals(1, perioderMedOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 3)), perioderMedOverlapp[0])
        assertEquals(1, perioderUtenOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 12)), perioderUtenOverlapp[0])

        perioder = periode.perioderMedOgUtenOverlapp(
            listOf(
                MånedPeriode(YearMonth.of(2015, 1), YearMonth.of(2018, 3)),
                MånedPeriode(YearMonth.of(2018, 6), YearMonth.of(2018, 6)),
            ),
        )
        perioderMedOverlapp = perioder.first
        perioderUtenOverlapp = perioder.second
        assertEquals(2, perioderMedOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 3)), perioderMedOverlapp[0])
        assertEquals(MånedPeriode(YearMonth.of(2018, 6), YearMonth.of(2018, 6)), perioderMedOverlapp[1])
        assertEquals(2, perioderUtenOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 4), YearMonth.of(2018, 5)), perioderUtenOverlapp[0])
        assertEquals(MånedPeriode(YearMonth.of(2018, 7), YearMonth.of(2018, 12)), perioderUtenOverlapp[1])

        perioder = periode.perioderMedOgUtenOverlapp(
            listOf(
                MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 11)),
            ),
        )
        perioderMedOverlapp = perioder.first
        perioderUtenOverlapp = perioder.second
        assertEquals(1, perioderMedOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 2), YearMonth.of(2018, 11)), perioderMedOverlapp[0])
        assertEquals(2, perioderUtenOverlapp.size)
        assertEquals(MånedPeriode(YearMonth.of(2018, 1), YearMonth.of(2018, 1)), perioderUtenOverlapp[0])
        assertEquals(MånedPeriode(YearMonth.of(2018, 12), YearMonth.of(2018, 12)), perioderUtenOverlapp[1])
    }

    private fun verifiserAndelTilkjentYtelse(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        forventetBarnIdent: String,
        forventetBeløp: BigDecimal,
        forventetStønadFom: YearMonth,
        forventetStønadTom: YearMonth,
    ) {
        assertEquals(forventetBarnIdent, andelTilkjentYtelse.aktør.aktivFødselsnummer())
        assertEquals(forventetBeløp, BigDecimal(andelTilkjentYtelse.kalkulertUtbetalingsbeløp))
        assertEquals(forventetStønadFom, andelTilkjentYtelse.stønadFom)
        assertEquals(forventetStønadTom, andelTilkjentYtelse.stønadTom)
    }

    private fun lagAndelTilkjentYtelse(barn: Person, fom: YearMonth, tom: YearMonth) = AndelTilkjentYtelse(
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        aktør = barn.aktør,
        kalkulertUtbetalingsbeløp = beløp.toInt(),
        nasjonaltPeriodebeløp = beløp.toInt(),
        stønadFom = fom,
        stønadTom = tom,
        type = YtelseType.ORDINÆR_BARNETRYGD,
        sats = beløp.toInt(),
        prosent = BigDecimal(100),
    )
}
