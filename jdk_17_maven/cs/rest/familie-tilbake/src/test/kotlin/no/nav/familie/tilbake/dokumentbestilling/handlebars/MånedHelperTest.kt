package no.nav.familie.tilbake.dokumentbestilling.handlebars

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class MånedHelperTest {

    @Test
    fun `apply returnerer alle måneder på korrekt format`() {
        val januar = MånedHelper().apply(YearMonth.of(2022, 1), null)
        val februar = MånedHelper().apply(YearMonth.of(2022, 2), null)
        val mars = MånedHelper().apply(YearMonth.of(2022, 3), null)
        val april = MånedHelper().apply(YearMonth.of(2022, 4), null)
        val mai = MånedHelper().apply(YearMonth.of(2022, 5), null)
        val juni = MånedHelper().apply(YearMonth.of(2022, 6), null)
        val juli = MånedHelper().apply(YearMonth.of(2022, 7), null)
        val august = MånedHelper().apply(YearMonth.of(2022, 8), null)
        val september = MånedHelper().apply(YearMonth.of(2022, 9), null)
        val oktober = MånedHelper().apply(YearMonth.of(2022, 10), null)
        val november = MånedHelper().apply(YearMonth.of(2022, 11), null)
        val desember = MånedHelper().apply(YearMonth.of(2022, 12), null)

        januar shouldBe "januar 2022"
        februar shouldBe "februar 2022"
        mars shouldBe "mars 2022"
        april shouldBe "april 2022"
        mai shouldBe "mai 2022"
        juni shouldBe "juni 2022"
        juli shouldBe "juli 2022"
        august shouldBe "august 2022"
        september shouldBe "september 2022"
        oktober shouldBe "oktober 2022"
        november shouldBe "november 2022"
        desember shouldBe "desember 2022"
    }
}
