package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.Innhold
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidslinjeFraTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.okt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TidslinjeFraTidspunktTest {
    val tidslinje = tidslinje {
        listOf(
            Periode(mar(2018), mar(2018), null),
            Periode(mai(2018), mai(2018), "A"),
            Periode(jun(2018), sep(2018), "B"),
            Periode(des(2018), feb(2019), "C"),
            Periode(apr(2019), jul(2019), null),
            Periode(sep(2019), jan(2020), "D"),
            Periode(feb(2020), okt(2020), null),
            Periode(nov(2020), feb(2021), "e"),
            Periode(apr(2021), apr(2021), null),
        )
    }

    @Test
    fun `skal gjenskape underliggende tidslinje dersom innholdet returneres uendret `() {
        val resultat = tidslinje.tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
            tidslinje.innholdForTidspunkt(tidspunkt)
        }

        assertEquals(tidslinje, resultat)
    }

    @Test
    fun `skal skape sammenhengende tidslinje i samme tidsrom hvis alt innhold er identisk`() {
        val resultat = tidslinje.tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
            Innhold("A")
        }

        val forventet = tidslinje { listOf(Periode(mar(2018), apr(2021), "A")) }

        assertEquals(forventet, resultat)
    }

    @Test
    fun `skal skape tom tidslinje dersom alt innhold mangler`() {
        val resultat = tidslinje.tidsrom().tidslinjeFraTidspunkt { tidspunkt ->
            Innhold.utenInnhold<String>()
        }

        assertEquals(TomTidslinje<String, Måned>(), resultat)
    }
}
