package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.Innhold
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
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

internal class MapTidslinjeTest {

    val tidslinje = tidslinje {
        listOf(
            Periode(aug(2019), nov(2019), null),
            Periode(jan(2020), mar(2020), "A"),
            Periode(apr(2020), jun(2020), null),
            Periode(jul(2020), aug(2020), "B"),
            Periode(mar(2021), okt(2021), "C"),
            Periode(jan(2022), mai(2022), null),
        )
    }

    @Test
    fun `skal mappe innhold og ivareta null`() {
        val faktisk = tidslinje.map { it?.lowercase() }

        val forventet = tidslinje {
            listOf(
                Periode(aug(2019), nov(2019), null),
                Periode(jan(2020), mar(2020), "a"),
                Periode(apr(2020), jun(2020), null),
                Periode(jul(2020), aug(2020), "b"),
                Periode(mar(2021), okt(2021), "c"),
                Periode(jan(2022), mai(2022), null),
            )
        }

        assertEquals(forventet, faktisk)
        assertEquals(aug(2019), faktisk.fraOgMed())
        assertEquals(mai(2022), faktisk.tilOgMed())
    }

    @Test
    fun `skal mappe innhold og fjerne null`() {
        val faktisk = tidslinje.mapIkkeNull { it.lowercase() }

        val forventet = tidslinje {
            listOf(
                Periode(jan(2020), mar(2020), "a"),
                Periode(jul(2020), aug(2020), "b"),
                Periode(mar(2021), okt(2021), "c"),
            )
        }

        assertEquals(forventet, faktisk)
        assertEquals(jan(2020), faktisk.fraOgMed())
        assertEquals(okt(2021), faktisk.tilOgMed())

        (
            (aug(2019)..des(2019))
                .plus(apr(2020)..jun(2020))
                .plus(sep(2020)..feb(2021))
                .plus(nov(2021)..mai(2022))
            ).forEach {
            assertEquals(Innhold.utenInnhold<String>(), faktisk.innholdForTidspunkt(it))
        }
    }
}
