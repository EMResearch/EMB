package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TomTidslinjeTest {

    @Test
    fun `test at fra-og-med og til-og-med er uendelige`() {
        val fom = TomTidslinje<Boolean, Dag>().fraOgMed()
        val tom = TomTidslinje<Boolean, Dag>().tilOgMed()

        assertNull(fom)
        assertNull(tom)
    }

    @Test
    fun `test at tidsrommet mellom fra-og-med og til-og-med er tomt`() {
        assertTrue(TomTidslinje<Boolean, Dag>().tidsrom().toList().isEmpty())
    }

    @Test
    fun `test at listen av perioder er tom`() {
        assertTrue(TomTidslinje<Boolean, Dag>().perioder().isEmpty())
    }

    @Test
    fun `test kombinering av to tomme tidslinjer`() {
        val resultat =
            TomTidslinje<Boolean, Måned>().kombinerMed(TomTidslinje<Boolean, Måned>()) { v, h -> v ?: h }

        assertEquals(TomTidslinje<Boolean, Måned>(), resultat)
    }

    @Test
    fun `test kombinering fra tom tidslinje til tidslinje med innhold`() {
        val boolskTidslinje = "tftftftftftft".somBoolskTidslinje(jan(2020))
        val resultat = TomTidslinje<Boolean, Måned>().kombinerMed(boolskTidslinje) { v, h -> v ?: h }

        assertEquals(boolskTidslinje, resultat)
    }

    @Test
    fun `test kombinering fra tidslinje med innhold til tom tidslinje`() {
        val boolskTidslinje = "tftft    ftft".somBoolskTidslinje(jan(2020))
        val resultat = boolskTidslinje.kombinerMed(TomTidslinje<Boolean, Måned>()) { v, h -> v ?: h }

        assertEquals(boolskTidslinje, resultat)
    }
}
