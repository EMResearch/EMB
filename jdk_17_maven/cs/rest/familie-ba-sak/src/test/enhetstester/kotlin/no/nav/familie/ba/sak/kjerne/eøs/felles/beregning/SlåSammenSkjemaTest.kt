package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SlåSammenSkjemaTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testSlåSammenPåfølgendePerioder() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS", barn1)
            .medKompetanse("   SSS", barn1)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSS", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testSlåSammenForPerioderMedMellomrom() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS", barn1, barn2, barn3)
            .medKompetanse("    SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testSlåSammenForPerioderDerTidligstePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        Assertions.assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun testSlåSammenForPerioderMedOverlapp() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("-----", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("--------", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testSlåSammneForPerioderDerSenestePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .medKompetanse("   ----->", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        Assertions.assertEquals(jan2020.tilYearMonth(), faktiskeKompetanser.first().fom)
        Assertions.assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun komplekseSlåSammenKommpetanserTest() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .medKompetanse("SSSPPSS", barn2)
            .medKompetanse("-SSSSSS", barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("       ", barn2, barn3)
            .medKompetanse("       ", barn1)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        Assertions.assertEquals(6, faktiskeKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun slåSammenEnkeltBarnSomSkillerSegHeltUt() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .medKompetanse("   SSSS", barn1)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.slåSammen()
        Assertions.assertEquals(2, faktiskeKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }
}
