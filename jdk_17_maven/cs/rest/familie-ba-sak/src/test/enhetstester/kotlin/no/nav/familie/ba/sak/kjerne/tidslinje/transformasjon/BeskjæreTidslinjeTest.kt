package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Uendelighet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

internal class BeskjæreTidslinjeTest {

    @Test
    fun `skal beskjære endelig tidslinje på begge sider`() {
        val hovedlinje = "aaaaaa".tilCharTidslinje(des(2001))
        val beskjæring = "bbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `skal beholde tidslinje som allerede er innenfor beskjæring`() {
        val hovedlinje = "aaa".tilCharTidslinje(feb(2002))
        val beskjæring = "bbbbbbbbb".tilCharTidslinje(des(2001))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `skal beholde tidslinje som er innenfor en uendelig beskjæring`() {
        val hovedlinje = "aaa".tilCharTidslinje(feb(2002))
        val beskjæring = "<b>".tilCharTidslinje(mar(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `beskjæring utenfor tidslinjen skal gi tom tidslinje`() {
        val hovedlinje = "aaaaaa".tilCharTidslinje(des(2001))
        val beskjæring = "bbb".tilCharTidslinje(feb(2009))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)

        assertEquals(TomTidslinje<Char, Måned>(), faktisk)
    }

    @Test
    fun `skal beskjære uendelig tidslinje begge veier mot endelig tidsline`() {
        val hovedlinje = "<aaaaaa>".tilCharTidslinje(des(2002))
        val beskjæring = "bbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
    }

    @Test
    fun `skal beskjære tidslinje som går fra uendelig lenge siden til et endelig tidspunkt i fremtiden`() {
        val hovedlinje = "<aaaaaa".tilCharTidslinje(des(2038))
        val beskjæring = "bbbbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaaaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
    }

    @Test
    fun `skal beskjære tidslinje som går fra et endelig tidspunkt i fortiden til uendelig lenge til`() {
        val hovedlinje = "aaaaaa>".tilCharTidslinje(des(1993))
        val beskjæring = "bbbbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "aaaaa".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
    }

    @Test
    fun `skal beskjære uendelig fremtid slik at den blir kortest mulig`() {
        val hovedlinje = "aaaaaa>".tilCharTidslinje(des(1993))
        val beskjæring = "bbb>".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = tidslinje { listOf(Periode(feb(2002), feb(2002).somUendeligLengeTil(), 'a')) }

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
        assertEquals(Uendelighet.FREMTID, faktisk.tilOgMed()?.uendelighet)
    }

    @Test
    fun `skal beskjære uendelig fortid slik at den inneholder tidligste fra-og-med, beskjæring er tidligst`() {
        val hovedlinje = "<a".tilCharTidslinje(des(2038))
        val beskjæring = "<bbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = tidslinje { listOf(Periode(mai(2002).somUendeligLengeSiden(), mai(2002), 'a')) }

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
        assertEquals(Uendelighet.FORTID, faktisk.fraOgMed()?.uendelighet)
    }

    @Test
    fun `skal beskjære uendelig fortid slik at den inneholder tidligste fra-og-med, beskjæring er senest`() {
        val hovedlinje = "<bbb".tilCharTidslinje(feb(2002))
        val beskjæring = "<a".tilCharTidslinje(des(2038))

        val faktisk = hovedlinje.beskjærEtter(beskjæring)
        val forventet = "<bbb".tilCharTidslinje(feb(2002))

        assertEquals(forventet, faktisk)
        assertEquals(forventet.somEndelig(), faktisk.somEndelig())
    }

    @Test
    fun `beskjære mot tom tidslinje skal gi tom tidslinje`() {
        val hovedlinje = "bbb".tilCharTidslinje(feb(2002))

        val faktisk = hovedlinje.beskjærEtter(TomTidslinje<Char, Måned>())
        val forventet = TomTidslinje<Char, Måned>()

        assertEquals(forventet, faktisk)
    }
}
