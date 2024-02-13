package no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

import no.nav.familie.ba.sak.kjerne.tidslinje.minsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.størsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TidsromTest {
    @Test
    fun testStørsteAv() {
        assertEquals(
            feb(2020),
            størsteAv(jan(2020), feb(2020)),
        )
        assertEquals(
            feb(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), jan(2020)),
        )
        assertEquals(
            jun(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), mai(2020)),
        )
        assertEquals(
            jan(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), des(2019)),
        )
        assertEquals(
            feb(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), feb(2020).somUendeligLengeTil()),
        )
    }

    @Test
    fun testMinsteAv() {
        assertEquals(
            des(2019).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), jan(2020)),
        )
        assertEquals(
            apr(2019).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), mai(2019)),
        )
        assertEquals(
            jan(2020).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), feb(2020)),
        )
        assertEquals(
            feb(2020).somUendeligLengeSiden(),
            størsteAv(feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden()),
        )
    }

    @Test
    fun `Equals på ulike tidsenheter skal være forskjellig`() {
        assertEquals(feb(2020), feb(2020))
        assertEquals(1.feb(2020), 1.feb(2020))
        assertEquals(31.jan(2020), 31.jan(2020))

        assertNotEquals(1.jan(2020), jan(2020))
        assertNotEquals(31.jan(2020), jan(2020))
    }

    @Test
    fun `Equals på samme uendelig skal være lik`() {
        assertEquals(feb(2020).somUendeligLengeTil(), mar(2020).somUendeligLengeTil())
        assertEquals(feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden())

        assertEquals(1.feb(2020).somUendeligLengeTil(), 2.feb(2020).somUendeligLengeTil())
        assertEquals(1.feb(2020).somUendeligLengeSiden(), 2.feb(2020).somUendeligLengeSiden())

        assertEquals(1.feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden())
        assertEquals(feb(2020).somUendeligLengeTil(), 1.jan(2020).somUendeligLengeTil())

        assertNotEquals(feb(2020).somUendeligLengeSiden(), feb(2020).somUendeligLengeTil())
        assertNotEquals(5.feb(2020).somUendeligLengeTil(), 5.feb(2020).somUendeligLengeSiden())
    }
}
