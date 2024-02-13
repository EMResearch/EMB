package no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TidspunktClosedRangeTest {
    val A = YearMonth.of(2020, 1).tilTidspunkt()
    val B = A.neste()
    val C = B.neste()
    val D = C.neste()
    val E = D.neste()
    val F = E.neste()

    val tomListe = emptyList<Tidspunkt<Måned>>()

    @Test
    fun `A til A`() {
        val tidsrom = A..A
        assertEquals(listOf(A), tidsrom.toList())
    }

    @Test
    fun `A til B`() {
        val tidsrom = A..B
        assertEquals(listOf(A, B), tidsrom.toList())
    }

    @Test
    fun `A til C`() {
        val tidsrom = A..C
        assertEquals(listOf(A, B, C), tidsrom.toList())
    }

    @Test
    fun `B til A`() {
        val tidsrom = B..A
        assertEquals(tomListe, tidsrom.toList())
    }

    @Test
    fun `←A til A`() {
        val tidspunkter = (A.somUendeligLengeSiden()..A).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
    }

    @Test
    fun `←A til B`() {
        val tidspunkter = (A.somUendeligLengeSiden()..B).toList()
        assertEquals(listOf(A, B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erEndelig())
    }

    @Test
    fun `←A til ←A`() {
        val tidspunkter = (A.somUendeligLengeSiden()..A.somUendeligLengeSiden()).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `←A til ←C`() {
        val tidspunkter = (A.somUendeligLengeSiden()..C.somUendeligLengeSiden()).toList()
        assertEquals(listOf(A, B, C), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
        assertTrue(tidspunkter[1].erEndelig())
        assertTrue(tidspunkter[2].erEndelig())
    }

    @Test
    fun `←B til A`() {
        val tidspunkter = (B.somUendeligLengeSiden()..A).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `←B til ←A`() {
        val tidspunkter = (B.somUendeligLengeSiden()..A.somUendeligLengeSiden()).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `A til A→`() {
        val tidspunkter = (A..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `A→ til A→`() {
        val tidspunkter = (A.somUendeligLengeTil()..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(A), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `A→ til C→`() {
        val tidspunkter = (A.somUendeligLengeTil()..C.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B, C), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erEndelig())
        assertTrue(tidspunkter[1].erEndelig())
        assertTrue(tidspunkter[2].erUendeligLengeTil())
    }

    @Test
    fun `B til A→`() {
        val tidspunkter = (B..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `B→ til A→`() {
        val tidspunkter = (B.somUendeligLengeTil()..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `←A til A→`() {
        val tidspunkter = (A.somUendeligLengeSiden()..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←A til B→`() {
        val tidspunkter = (A.somUendeligLengeSiden()..B.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←A til C→`() {
        val tidspunkter = (A.somUendeligLengeSiden()..C.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B, C), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←B til A→`() {
        val tidspunkter = (B.somUendeligLengeSiden()..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←E til A→`() {
        val tidspunkter = (E.somUendeligLengeSiden()..A.somUendeligLengeTil()).toList()
        assertEquals(listOf(A, B, C, D, E), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `A→ til ←A`() {
        val tidsrom = A.somUendeligLengeTil()..A.somUendeligLengeSiden()
        assertEquals(listOf(A), tidsrom.toList())
    }

    @Test
    fun `A→ til ←B`() {
        val tidsrom = A.somUendeligLengeTil()..B.somUendeligLengeSiden()
        assertEquals(listOf(A, B), tidsrom.toList())
    }

    @Test
    fun `A→ til ←E`() {
        val tidsrom = A.somUendeligLengeTil()..E.somUendeligLengeSiden()
        assertEquals(listOf(A, B, C, D, E), tidsrom.toList())
    }

    @Test
    fun `B→ til ←A`() {
        val tidsrom = B.somUendeligLengeTil()..A.somUendeligLengeSiden()
        assertEquals(tomListe, tidsrom.toList())
    }

    @Test
    fun testTidsromMedMåneder() {
        val fom = MånedTidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = MånedTidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        assertEquals(10, tidsrom.count())
        assertEquals(fom, tidsrom.first())
        assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testTidsromMedDager() {
        val fom = DagTidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1))
        val tom = DagTidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31))
        val tidsrom = fom..tom

        assertEquals(305, tidsrom.count())
        assertEquals(fom, tidsrom.first())
        assertEquals(tom, tidsrom.last())
    }
}
