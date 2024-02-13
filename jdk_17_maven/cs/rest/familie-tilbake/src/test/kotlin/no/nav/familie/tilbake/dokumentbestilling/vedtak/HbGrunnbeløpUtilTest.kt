package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.HbGrunnbeløpUtil.lagHbGrunnbeløp
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class HbGrunnbeløpUtilTest {

    @Test
    internal fun `skal bruke fra og til fra perioden hvis de er innenfor perioden sine datoer `() {
        val result = lagHbGrunnbeløp(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)))

        result.tekst6GangerGrunnbeløp shouldBe null
        result.grunnbeløpGanger6 shouldBe 608_106.toBigDecimal()
    }

    @Test
    internal fun `skal bruke periode sin fom hvis den er større enn beløpsperiode sin startdato`() {
        val result = lagHbGrunnbeløp(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 6)))

        result.tekst6GangerGrunnbeløp shouldBe "608 106 kroner for perioden 1. mars 2021 til 30. april 2021 og 638 394 kroner for perioden 1. mai 2021 til 30. juni 2021"
        result.grunnbeløpGanger6 shouldBe null
    }

    @Test
    internal fun `skal bruke periode sin fom hvis den er større enn beløpsperiode sin startdato 2`() {
        val result = lagHbGrunnbeløp(Månedsperiode(YearMonth.of(2021, 4), YearMonth.of(2021, 5))).tekst6GangerGrunnbeløp

        result shouldBe "608 106 kroner for perioden 1. april 2021 til 30. april 2021 " +
            "og 638 394 kroner for perioden 1. mai 2021 til 31. mai 2021"
    }

    @Test
    internal fun `periode og beløpsperiode er lik - gir kun en periode`() {
        val result = lagHbGrunnbeløp(Månedsperiode(YearMonth.of(2021, 5), YearMonth.of(2022, 4)))

        result.tekst6GangerGrunnbeløp shouldBe null
        result.grunnbeløpGanger6 shouldBe 638_394.toBigDecimal()
    }

    @Test
    internal fun `perioden går over 3 grunnbeløpsperioder`() {
        val result = lagHbGrunnbeløp(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2022, 6))).tekst6GangerGrunnbeløp

        result shouldBe "608 106 kroner for perioden 1. mars 2021 til 30. april 2021, " +
            "638 394 kroner for perioden 1. mai 2021 til 30. april 2022 " +
            "og 668 862 kroner for perioden 1. mai 2022 til 30. juni 2022"
    }
}
