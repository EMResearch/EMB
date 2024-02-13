package no.nav.familie.tilbake.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.Grunnbeløpsperioder.finnGrunnbeløpsperioder
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class GrunnbeløpsperioderTest {

    @Test
    internal fun `skal kaste feil hvis man ikke får noen treff på grunnbeløpsperioder bak i tiden`() {
        shouldThrow<IllegalArgumentException> {
            finnGrunnbeløpsperioder(Månedsperiode(YearMonth.of(1900, 1)))
        }.message shouldBe "Forventer å finne treff for 1900-01 - 1900-01 i grunnbeløpsperioder"
    }

    @Test
    internal fun `skal kaste feil når perioden sitt sluttdato er etter siste grunnbeløpet sin tom-dato`() {
        shouldThrow<IllegalArgumentException> {
            finnGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2300, 1)))
        }.message shouldBe "Har ikke lagt inn grunnbeløpsperiode frem til 2300-01"
    }

    @Test
    internal fun `skal finne en treff for enmåneds-perioder som ikke går over flere grunnbeløpsperioder`() {
        IntRange(5, 12).forEach { måned ->
            assertGrunnbeløp(Månedsperiode(YearMonth.of(2020, måned)), 101_351)
        }
        IntRange(1, 4).forEach { måned ->
            assertGrunnbeløp(Månedsperiode(YearMonth.of(2021, måned)), 101_351)
        }
        IntRange(5, 12).forEach { måned ->
            assertGrunnbeløp(Månedsperiode(YearMonth.of(2021, måned)), 106_399)
        }
    }

    @Test
    internal fun `skal finne en treff for periode som ikke går over flere grunnbeløpsperioder`() {
        assertGrunnbeløp(Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2021, 4)), 101_351)
        assertGrunnbeløp(Månedsperiode(YearMonth.of(2021, 5), YearMonth.of(2022, 4)), 106_399)
    }

    @Test
    internal fun `overlapper med 1 måned skal returnere 2 grunnbeløpsperioder`() {
        val fra = YearMonth.of(2020, 4)
        val til = YearMonth.of(2020, 5)
        val resultat = finnGrunnbeløpsperioder(Månedsperiode(fra, til))
        resultat shouldHaveSize 2
        resultat[0].grunnbeløp shouldBe 99_858.toBigDecimal()
        resultat[1].grunnbeløp shouldBe 101_351.toBigDecimal()
    }

    @Test
    internal fun `overlapper med flere måneder skal returnere 2 grunnbeløpsperioder`() {
        val fra = YearMonth.of(2019, 5)
        val til = YearMonth.of(2021, 4)
        val resultat = finnGrunnbeløpsperioder(Månedsperiode(fra, til))
        resultat shouldHaveSize 2
        resultat[0].grunnbeløp shouldBe 99_858.toBigDecimal()
        resultat[1].grunnbeløp shouldBe 101_351.toBigDecimal()
    }

    @Test
    internal fun `overlapper 3 grunnbeløpsperioder skal returnere 3 beløpsperioder`() {
        val fra = YearMonth.of(2019, 5)
        val til = YearMonth.of(2021, 5)
        val resultat = finnGrunnbeløpsperioder(Månedsperiode(fra, til))
        resultat shouldHaveSize 3
        resultat[0].grunnbeløp shouldBe 99_858.toBigDecimal()
        resultat[1].grunnbeløp shouldBe 101_351.toBigDecimal()
        resultat[2].grunnbeløp shouldBe 106_399.toBigDecimal()
    }

    private fun assertGrunnbeløp(periode: Månedsperiode, beløp: Int) {
        val resultat = finnGrunnbeløpsperioder(periode)
        resultat shouldHaveSize 1
        resultat[0].periode.inneholder(periode) shouldBe true
        resultat[0].grunnbeløp shouldBe beløp.toBigDecimal()
    }
}
