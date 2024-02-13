package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassUtenlandskePeriodebeløpTilKompetanser
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Test

/**
 * Syntaks:
 * ' ' (blank): Skjema finnes ikke for perioden
 * '-': Skjema finnes, men alle felter er null
 * '$': Skjema finnes, valutakode er satt, men ellers null-felter
 * '<siffer>': Skjema har oppgitt beløp og valutakode
 */
class TilpassUtenlandskePeriodebeløpTilKompetanserTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun `test tilpasning av utenlandske periodebeløp mot kompleks endring av kompetanse`() {
        val forrigeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan2020)
            .medBeløp("--3456789-----", "EUR", "N", barn1, barn2)
            .bygg()

        val gjeldendeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSPPPSSS", barn1, annenForeldersAktivitetsland = "N")
            .medKompetanse("PP--PP--PP", barn2, annenForeldersAktivitetsland = "N")
            .medKompetanse("-SSS-PP-S-", barn3, annenForeldersAktivitetsland = "N")
            .byggKompetanser()

        val forventedeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan2020)
            .medBeløp("- 34   89-", "EUR", "N", barn1)
            .medBeløp("  --    - ", null, "N", barn3)
            .medBeløp(" -        ", null, "N", barn1, barn3)
            .bygg()

        val faktiskeUtenlandskePeriodebeløp =
            tilpassUtenlandskePeriodebeløpTilKompetanser(forrigeUtenlandskePeriodebeløp, gjeldendeKompetanser)

        assertEqualsUnordered(forventedeUtenlandskePeriodebeløp, faktiskeUtenlandskePeriodebeløp)
    }

    @Test
    fun `test at endret annennForeldersAktivitetsland i kompetanse fører til endring i utenlandsk periodebeløp`() {
        val forrigeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan2020)
            .medBeløp("555555", "EUR", "N", barn1)
            .bygg()

        val gjeldendeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSS", barn1, annenForeldersAktivitetsland = "S")
            .byggKompetanser()

        val faktiskeUtenlandskePeriodebeløp =
            tilpassUtenlandskePeriodebeløpTilKompetanser(forrigeUtenlandskePeriodebeløp, gjeldendeKompetanser)

        val forventedeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan2020)
            .medBeløp("------", null, "S", barn1)
            .bygg()

        assertEqualsUnordered(forventedeUtenlandskePeriodebeløp, faktiskeUtenlandskePeriodebeløp)
    }
}
