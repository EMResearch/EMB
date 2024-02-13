package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utbetalingsperiodemedbegrunnelser

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtbetalingsperiodeMedBegrunnelserUtilsTest {

    val barnAktør1 = Aktør(aktørId = "1111111111111")
    val barnAktør2 = Aktør(aktørId = "2222222222222")

    @Test
    fun `skal splitte opp utbetalingsperioder når det er overlappende kompetanse`() {
        val periode1 = MånedPeriode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        val periode2 = MånedPeriode(YearMonth.of(2021, 2), YearMonth.of(2021, 3))
        val periode3 = MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 5))
        val periode4 = MånedPeriode(YearMonth.of(2021, 6), YearMonth.of(2021, 7))
        val periode5 = MånedPeriode(YearMonth.of(2021, 8), YearMonth.of(2021, 10))

        val utbetalingsperiode1 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = periode1.fom.førsteDagIInneværendeMåned(),
            tom = periode2.tom.sisteDagIInneværendeMåned(),
        )

        val kompetanse1 = lagKompetanse(fom = periode2.fom, tom = periode2.tom, barnAktører = setOf(barnAktør1))

        val utbetalingsperiode2 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = periode3.fom.førsteDagIInneværendeMåned(),
            tom = periode4.tom.sisteDagIInneværendeMåned(),
        )

        val kompetanse2 = lagKompetanse(fom = periode4.fom, tom = periode5.tom, barnAktører = setOf(barnAktør1))

        val utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            fom = periode5.fom.førsteDagIInneværendeMåned(),
            tom = periode5.tom.sisteDagIInneværendeMåned(),
        )

        val utbetalingsperiodeMedKompetanseSplitter = splittUtbetalingsperioderPåKompetanser(
            utbetalingsperioder = listOf(
                utbetalingsperiode1,
                utbetalingsperiode2,
                utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling,
            ),
            kompetanser = listOf(kompetanse1, kompetanse2),
        )

        val forventedePerioder =
            listOf(periode1, periode2, periode3, periode4, periode5)

        Assertions.assertEquals(5, utbetalingsperiodeMedKompetanseSplitter.size)
        utbetalingsperiodeMedKompetanseSplitter
            .zip(forventedePerioder)
            .forEach { (utbetalingsperiode, forventetPeriode) ->
                Assertions.assertEquals(forventetPeriode.fom.førsteDagIInneværendeMåned(), utbetalingsperiode.fom)
                Assertions.assertEquals(forventetPeriode.tom.sisteDagIInneværendeMåned(), utbetalingsperiode.tom)
            }
    }

    @Test
    fun `Skal splitte opp utbetalingsperioder riktig når kompetanse strekker seg over flere perioder`() {
        val utbetalingsperioder = listOf(
            MånedPeriode(YearMonth.of(2020, 5), YearMonth.of(2020, 8)),
            MånedPeriode(YearMonth.of(2020, 9), YearMonth.of(2021, 4)),
            MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 8)),
            MånedPeriode(YearMonth.of(2021, 9), YearMonth.of(2021, 12)),
            MånedPeriode(YearMonth.of(2022, 1), YearMonth.of(2022, 3)),
            MånedPeriode(YearMonth.of(2022, 4), YearMonth.of(2038, 3)),
        ).mapIndexed { _, it ->
            lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.UTBETALING,
                fom = it.fom.førsteDagIInneværendeMåned(),
                tom = it.tom.sisteDagIInneværendeMåned(),
            )
        }

        val kompetanse =
            lagKompetanse(fom = YearMonth.of(2020, 5), tom = YearMonth.of(2021, 5), barnAktører = setOf(barnAktør1))

        val utbetalingsperiodeMedKompetanseSplitter = splittUtbetalingsperioderPåKompetanser(
            utbetalingsperioder = utbetalingsperioder,
            kompetanser = listOf(kompetanse),
        )

        Assertions.assertEquals(7, utbetalingsperiodeMedKompetanseSplitter.size)
    }

    @Test
    fun `Skal splitte opp utbetalingsperiodene riktig når det er kompetanse på forskjellige personer`() {
        val periode1 = MånedPeriode(YearMonth.of(2021, 1), YearMonth.of(2021, 4))
        val periode2 = MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 6))
        val periode3 = MånedPeriode(YearMonth.of(2021, 7), YearMonth.of(2022, 1))

        val utbetalingsperiode1 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = periode1.fom.førsteDagIInneværendeMåned(),
            tom = periode3.tom.sisteDagIInneværendeMåned(),
        )

        val kompetanse1 =
            lagKompetanse(fom = periode1.fom, tom = periode2.tom, barnAktører = setOf(barnAktør1))
        val kompetanse2 =
            lagKompetanse(fom = periode2.fom, tom = periode3.tom, barnAktører = setOf(barnAktør2))

        val forventedePerioder =
            listOf(periode1, periode2, periode3)

        val utbetalingsperiodeMedKompetanseSplitter = splittUtbetalingsperioderPåKompetanser(
            utbetalingsperioder = listOf(utbetalingsperiode1),
            kompetanser = listOf(kompetanse1, kompetanse2),
        )

        Assertions.assertEquals(3, utbetalingsperiodeMedKompetanseSplitter.size)
        utbetalingsperiodeMedKompetanseSplitter
            .zip(forventedePerioder)
            .forEach { (utbetalingsperiode, forventetPeriode) ->
                Assertions.assertEquals(forventetPeriode.fom.førsteDagIInneværendeMåned(), utbetalingsperiode.fom)
                Assertions.assertEquals(forventetPeriode.tom.sisteDagIInneværendeMåned(), utbetalingsperiode.tom)
            }
    }
}
