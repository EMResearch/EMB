package no.nav.familie.tilbake.dokumentbestilling.handlebars

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class KroneFormattererMedTusenskilleTest {

    @Test
    fun `medTusenskille skal gi riktig tusenskille`() {
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1), ' ') shouldBe "1"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12), ' ') shouldBe "12"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123), ' ') shouldBe "123"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234), ' ') shouldBe "1 234"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12345), ' ') shouldBe "12 345"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123456), ' ') shouldBe "123 456"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234567), ' ') shouldBe "1 234 567"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12345678), ' ') shouldBe "12 345 678"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123456789), ' ') shouldBe "123 456 789"
        KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234567), '\u00A0') shouldBe "1\u00A0234\u00A0567"
    }
}
