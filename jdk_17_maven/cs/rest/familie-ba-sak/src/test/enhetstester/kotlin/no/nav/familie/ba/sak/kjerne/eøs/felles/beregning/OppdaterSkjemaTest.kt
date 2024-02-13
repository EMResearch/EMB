package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Test

internal class OppdaterSkjemaTest {
    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val barn3 = tilfeldigPerson(personType = PersonType.BARN)

    val kompetanser = KompetanseBuilder(jan(2020))
        .medKompetanse("    SSSSSPP", barn1)
        .medKompetanse("  ---------", barn2, barn3)
        .medKompetanse("           PPPP", barn1, barn2, barn3)
        .byggKompetanser()

    @Test
    fun `oppdatere med tom kompetanse skal ikke har noen effekt`() {
        val tomKompetanse = Kompetanse(null, null)

        val faktiskeKompetanser = oppdaterSkjemaerRekursivt(kompetanser, tomKompetanse)
        assertEqualsUnordered(kompetanser, faktiskeKompetanser)
    }

    @Test
    fun `oppdatere tom liste av kompetansr med en gyldig kompetanse skal gi tom liste`() {
        val kompetanse = kompetanse(jan(2020), "------", barn1, barn2, barn3)

        val faktiskeKompetanser = oppdaterSkjemaerRekursivt(emptyList(), kompetanse)
        assertEqualsUnordered(emptyList(), faktiskeKompetanser)
    }

    @Test
    fun `oppdatere utenfor gjeldende kompetanser skal ikke ha effekt`() {
        val kompetanse = kompetanse(
            jan(2019),
            "---SSS                               PPP------",
            barn1,
            barn2,
            barn3,
        )

        val faktiskeKompetanser = oppdaterSkjemaerRekursivt(kompetanser, kompetanse)
        assertEqualsUnordered(kompetanser, faktiskeKompetanser)
    }

    @Test
    fun `oppdatere mer enn gjeldende kompetanser skal bare påvirke eksisterende tidsperioder`() {
        val kompetanse = kompetanse(jan(2020), "PPPPPPPPPPPPPPPPPPPPPP", barn1, barn2, barn3)

        val forventedeKompetanser = KompetanseBuilder(jan(2020))
            .medKompetanse("    PPPPPPPPPPP", barn1, barn2, barn3)
            .medKompetanse("  PP", barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = oppdaterSkjemaerRekursivt(kompetanser, kompetanse)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `oppdatere kompetanser som begynner uendret, skal likevel bli endret`() {
        val kompetanse = kompetanse(jan(2020), "    SSSSSSSSS", barn1)

        val forventedeKompetanser = KompetanseBuilder(jan(2020))
            .medKompetanse("    SSSSSSSSS", barn1)
            .medKompetanse("  ---------PP", barn2, barn3)
            .medKompetanse("             PP", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = oppdaterSkjemaerRekursivt(kompetanser, kompetanse)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }
}
