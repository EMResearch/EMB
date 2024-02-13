package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsServiceTest {

    @Test
    fun `Skal opprette korrekt tidslinje for ordinær barnetrygd for barn`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2017, 4, 6))

        val ordinærTidslinje = lagOrdinærTidslinje(barn)
        val ordinærePerioder = ordinærTidslinje.perioder().toList()

        Assertions.assertEquals(8, ordinærePerioder.size)

        assertPeriode(TestKrPeriode(beløp = 970, fom = "2017-04", tom = "2019-02"), ordinærePerioder[0])
        assertPeriode(TestKrPeriode(beløp = 1054, fom = "2019-03", tom = "2020-08"), ordinærePerioder[1])
        assertPeriode(TestKrPeriode(beløp = 1354, fom = "2020-09", tom = "2021-08"), ordinærePerioder[2])
        assertPeriode(TestKrPeriode(beløp = 1654, fom = "2021-09", tom = "2021-12"), ordinærePerioder[3])
        assertPeriode(TestKrPeriode(beløp = 1676, fom = "2022-01", tom = "2023-02"), ordinærePerioder[4])
        assertPeriode(TestKrPeriode(beløp = 1723, fom = "2023-03", tom = "2023-03"), ordinærePerioder[5])
        assertPeriode(TestKrPeriode(beløp = 1083, fom = "2023-04", tom = "2023-06"), ordinærePerioder[6])
        assertPeriode(TestKrPeriode(beløp = 1310, fom = "2023-07", tom = null), ordinærePerioder[7])
    }

    private fun assertPeriode(
        forventet: TestKrPeriode,
        faktisk: no.nav.familie.ba.sak.kjerne.tidslinje.Periode<Int, Måned>,
    ) {
        Assertions.assertEquals(forventet.beløp, faktisk.innhold, "Forskjell i beløp")
        Assertions.assertEquals(
            forventet.fom?.let { årMnd(it) },
            faktisk.fraOgMed.tilYearMonthEllerNull(),
            "Forskjell i fra-og-med",
        )
        Assertions.assertEquals(
            forventet.tom?.let { årMnd(it) },
            faktisk.tilOgMed.tilYearMonthEllerNull(),
            "Forskjell i til-og-med",
        )
    }

    private data class TestKrPeriode(
        val beløp: Int,
        val fom: String?,
        val tom: String?,
    )
}
