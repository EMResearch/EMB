package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje.Companion.TidslinjeFeilException
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class TidslinjeTest {

    @Test
    fun `skal validere at perioder ikke kan ha fra-og-med etter til-og-med`() {
        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(15.jan(2020), 14.jan(2020), 'A'),
            ).perioder()
        }
    }

    @Test
    fun `skal validere at perioder som overlapper med kun én dag ikke er lov`() {
        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(1.jan(2020), 31.mar(2020), 'A'),
                Periode(31.mar(2020), 31.mai(2020), 'B'),
            ).perioder()
        }
    }

    @Test
    fun `skal validere at periode som ligger inni en annen ikke er lov`() {
        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(1.jan(2020), 31.mai(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B'),
            ).perioder()
        }
    }

    @Test
    fun `skal validere at uendelig i begge ender av tidslinjen er lov`() {
        assertDoesNotThrow {
            TestTidslinje(
                Periode(1.jan(2020).somUendeligLengeSiden(), 1.jan(2020).somUendeligLengeTil(), 'A'),
            ).perioder()
        }

        assertDoesNotThrow {
            TestTidslinje(
                Periode(1.jan(2020).somUendeligLengeSiden(), 29.feb(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020).somUendeligLengeTil(), 'B'),
            ).perioder()
        }
    }

    @Test
    fun `skal validere at uendelige perioder inni en tidslinje ikke er lov`() {
        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(1.jan(2020), 31.jan(2020), 'A'),
                Periode(1.feb(2020).somUendeligLengeSiden(), 29.feb(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B'),
            ).perioder()
        }

        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(1.jan(2020), 31.jan(2020), 'A'),
                Periode(1.feb(2020), 29.feb(2020).somUendeligLengeTil(), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B'),
            ).perioder()
        }
    }

    @Test
    fun `skal presentere tidslinjefeil på et forstålig format`() {
        assertThatExceptionOfType(TidslinjeFeilException::class.java).isThrownBy {
            TestTidslinje(
                Periode(1.jan(2020), 31.jan(2020), 'A'),
                Periode(1.feb(2020), 29.feb(2020).somUendeligLengeTil(), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B'),
            ).perioder()
        }.withMessage(
            "[TidslinjeFeil(type=UENDELIG_FREMTID_FØR_SISTE_PERIODE, periode=2020-02-01 - 2020-02-29-->: A, tidslinje=2020-01-01 - 2020-01-31: A | 2020-02-01 - 2020-02-29-->: A | 2020-03-01 - 2020-04-30: B)]",
        )
    }

    @Test
    fun `Skal kunne kombinere tidslinje med uendelighet der det uendelige tidspunktet er satt tilbake i tid`() {
        val tidslinjeMedUendelighet = listOf(
            Periode(jan(2020), des(2020), 'A'),
            Periode(jan(2021), feb(1999).somUendeligLengeTil(), 'B'),
        ).tilTidslinje()

        val kombinertMedSegSelv = tidslinjeMedUendelighet.kombinerMed(tidslinjeMedUendelighet) { v, h ->
            "$v$h"
        }

        Assertions.assertThat(kombinertMedSegSelv).isEqualTo(
            listOf(
                Periode(jan(2020), des(2020), "AA"),
                Periode(jan(2021), feb(1999).somUendeligLengeTil(), "BB"),
            ).tilTidslinje(),
        )
    }

    @Test
    fun `tidsrom skal lage liste med alle tidspunkter opp til uendelig tidspunk`() {
        val tidslinjeMedUendelighet = listOf(
            Periode(nov(2020), des(2020), 'A'),
            Periode(jan(2021), jan(1999).somUendeligLengeTil(), 'B'),
        ).tilTidslinje()

        Assertions.assertThat(tidsrom(tidslinjeMedUendelighet).toList()).isEqualTo(
            listOf(nov(2020), des(2020), jan(1999).somUendeligLengeTil()),
        )
    }
}

internal class TestTidslinje(vararg val perioder: Periode<Char, Dag>) : Tidslinje<Char, Dag>() {
    override fun lagPerioder() = perioder.toList()
}
