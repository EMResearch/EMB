package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.ekstern.restDomene.tilKompetanse
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKompetanse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseMappingTest {

    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun sjekkAtMappingFremOgTilbakeGirSammeResultat() {
        val barnAktører = setOf(barn1.aktør, barn2.aktør, barn3.aktør)
        val kompetanse = Kompetanse(
            fom = YearMonth.of(2019, 4),
            tom = YearMonth.of(2037, 3),
            barnAktører = barnAktører,
            søkersAktivitet = KompetanseAktivitet.MOTTAR_PENSJON,
            annenForeldersAktivitet = KompetanseAktivitet.FORSIKRET_I_BOSTEDSLAND,
            annenForeldersAktivitetsland = "pl",
            barnetsBostedsland = "dk",
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
        )

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(kompetanse, restKompetanse.tilKompetanse(barnAktører.toList()))
    }
}
