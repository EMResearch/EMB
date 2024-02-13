package no.nav.familie.tilbake.avstemming

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FilMapperTest {

    @Test
    fun `tilFlatfil skal liste ut med forventet format for datoer og tall skal multipliseres med 100`() {
        val avstemmingsfil = FilMapper(listOf(testRad()))

        avstemmingsfil.tilFlatfil()
            .decodeToString() shouldBe FORVENTET_HEADER + "familie-tilbake;1234;12345678901;20191231;BA;1000;200;800;100;"
    }

    @Test
    fun `tilFlatfil skal ha newline for å skille rader`() {
        val avstemmingsfil = FilMapper(listOf(testRad(), testRad()))
        val enRad = "familie-tilbake;1234;12345678901;20191231;BA;1000;200;800;100;"
        avstemmingsfil.tilFlatfil().decodeToString() shouldBe "$FORVENTET_HEADER$enRad\n$enRad"
    }

    @Test
    fun `tilFlatfil skal bruke kode i siste kolonne når det er omgjøring til ingen tilbakekreving`() {
        val avstemmingsfil = FilMapper(
            listOf(
                Rad(
                    avsender = "familie-tilbake",
                    vedtakId = "1234",
                    fnr = "12345678901",
                    vedtaksdato = LocalDate.of(2019, 12, 31),
                    fagsakYtelseType = Ytelsestype.BARNETRYGD,
                    tilbakekrevesBruttoUtenRenter = BigDecimal.ZERO,
                    tilbakekrevesNettoUtenRenter = BigDecimal.ZERO,
                    renter = BigDecimal.ZERO,
                    skatt = BigDecimal.ZERO,
                    erOmgjøringTilIngenTilbakekreving = true,
                ),
            ),
        )
        avstemmingsfil.tilFlatfil()
            .decodeToString() shouldBe FORVENTET_HEADER + "familie-tilbake;1234;12345678901;20191231;BA;0;0;0;0;Omgjoring0"
    }

    private fun testRad(): Rad {
        return Rad(
            avsender = "familie-tilbake",
            vedtakId = "1234",
            fnr = "12345678901",
            vedtaksdato = LocalDate.of(2019, 12, 31),
            fagsakYtelseType = Ytelsestype.BARNETRYGD,
            tilbakekrevesBruttoUtenRenter = BigDecimal.valueOf(1000),
            tilbakekrevesNettoUtenRenter = BigDecimal.valueOf(800),
            skatt = BigDecimal.valueOf(200),
            renter = BigDecimal.valueOf(100),
            erOmgjøringTilIngenTilbakekreving = false,
        )
    }

    companion object {

        private const val FORVENTET_HEADER =
            "avsender;vedtakId;fnr;vedtaksdato;fagsakYtelseType;tilbakekrevesBruttoUtenRenter;skatt;" +
                "tilbakekrevesNettoUtenRenter;renter;erOmgjøringTilIngenTilbakekreving\n"
    }
}
