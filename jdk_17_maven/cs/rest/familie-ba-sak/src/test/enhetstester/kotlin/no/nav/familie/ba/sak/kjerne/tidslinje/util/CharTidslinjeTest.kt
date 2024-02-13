package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CharTidslinjeTest {
    @Test
    fun testEnkelCharTidsline() {
        val tegn = "---------------"
        val charTidslinje = CharTidslinje(tegn, jan(2020))
        Assertions.assertEquals(tegn.length, charTidslinje.perioder().size)

        val perioder = charTidslinje.slåSammenLike().perioder()
        Assertions.assertEquals(1, perioder.size)

        val periode = perioder.first()
        Assertions.assertEquals(jan(2020), periode.fraOgMed)
        Assertions.assertEquals(mar(2021), periode.tilOgMed)
        Assertions.assertEquals('-', periode.innhold)
    }

    @Test
    fun testUendeligCharTidslinje() {
        val tegn = "<--->"
        val charTidslinje = CharTidslinje(tegn, jan(2020))

        Assertions.assertEquals(tegn.length, charTidslinje.perioder().size)

        val perioder = charTidslinje.slåSammenLike().perioder()

        Assertions.assertEquals(1, perioder.size)
        val periode = perioder.first()
        Assertions.assertTrue(periode.fraOgMed.erUendeligLengeSiden())
        Assertions.assertTrue(periode.tilOgMed.erUendeligLengeTil())
        Assertions.assertEquals(jan(2020), periode.fraOgMed.somEndelig())
        Assertions.assertEquals(mai(2020), periode.tilOgMed.somEndelig())
        Assertions.assertEquals('-', periode.innhold)
    }

    @Test
    fun testSammensattTidsline() {
        val tegn = "aabbbbcdddddda"
        val charTidslinje = CharTidslinje(tegn, jan(2020))
        Assertions.assertEquals(tegn.length, charTidslinje.perioder().size)
        val perioder = charTidslinje.slåSammenLike().perioder().toList()
        Assertions.assertEquals(5, perioder.size)
        Assertions.assertEquals((jan(2020)..feb(2020)).med('a'), perioder[0])
        Assertions.assertEquals((mar(2020)..jun(2020)).med('b'), perioder[1])
        Assertions.assertEquals((jul(2020)..jul(2020)).med('c'), perioder[2])
        Assertions.assertEquals((aug(2020)..jan(2021)).med('d'), perioder[3])
        Assertions.assertEquals((feb(2021)..feb(2021)).med('a'), perioder[4])
    }
}
