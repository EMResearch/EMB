package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TrekkFraSkjemaTest {

    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testRestSomIntrodusererHull() {
        val kompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .byggKompetanser().first()

        val oppdatertKompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("  SS  ", barn1)
            .byggKompetanser().first()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn2, barn3)
            .medKompetanse("--  --", barn1)
            .byggKompetanser()

        val restKompetanser = kompetanse.trekkFra(oppdatertKompetanse)

        Assertions.assertEquals(3, restKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, restKompetanser)
    }

    @Test
    fun testRestMedPeriodeOverEnEnkeltMåned() {
        val kompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("  --", barn1, barn2)
            .byggKompetanser().first()

        val fjernKompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("   S", barn1)
            .byggKompetanser().first()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("  - ", barn1)
            .medKompetanse("  --", barn2)
            .byggKompetanser()

        val restKompetanser = kompetanse.trekkFra(fjernKompetanse)

        assertEqualsUnordered(forventedeKompetanser, restKompetanser)
    }
}
