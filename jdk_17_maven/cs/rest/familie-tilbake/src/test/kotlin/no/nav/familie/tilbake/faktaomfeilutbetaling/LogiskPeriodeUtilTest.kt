package no.nav.familie.tilbake.faktaomfeilutbetaling

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

internal class LogiskPeriodeUtilTest {

    private val januar = YearMonth.of(2021, 1)
    private val februar = YearMonth.of(2021, 2)
    private val mars = YearMonth.of(2021, 3)
    private val april = YearMonth.of(2021, 4)
    private val mai = YearMonth.of(2021, 5)

    @Test
    fun `utledLogiskPeriode skal returnere én logisk periode når perioder kan slås sammen`() {
        val periode1 = Månedsperiode(januar, februar)
        val periode2 = Månedsperiode(mars, mai)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(
            mapOf(
                periode1 to BigDecimal.valueOf(100),
                periode2 to BigDecimal.valueOf(200),
            ).toSortedMap(),
        )

        resultat.size shouldBe 1
        resultat[0].fom shouldBe januar
        resultat[0].tom shouldBe mai
        resultat[0].feilutbetaltBeløp shouldBe BigDecimal.valueOf(300)
    }

    @Test
    fun `utledLogiskPeriode skal returner flere logiske periode når perioder som er skilt med måned ikke kan slås sammen`() {
        val periode1 = Månedsperiode(januar, februar)
        val periode2 = Månedsperiode(april, mai)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(
            mapOf(
                periode1 to BigDecimal.valueOf(100),
                periode2 to BigDecimal.valueOf(200),
            ).toSortedMap(),
        )

        resultat.size shouldBe 2
        resultat[0].fom shouldBe januar
        resultat[0].tom shouldBe februar
        resultat[0].feilutbetaltBeløp shouldBe BigDecimal.valueOf(100)

        resultat[1].fom shouldBe april
        resultat[1].tom shouldBe mai
        resultat[1].feilutbetaltBeløp shouldBe BigDecimal.valueOf(200)
    }
}
