package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.KombinertRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilAnnenForelderOmfattetAvNorskLovgivningTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkResultatTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilpassKompetanserTilRegelverkTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testTilpassKompetanserUtenKompetanser() {
        val kompetanser: List<Kompetanse> = emptyList()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020).kombinertSøkersResultatTidslinje(),
        )
        val annenForelderOmfattetTidslinje =
            "++++-----++++++".tilAnnenForelderOmfattetAvNorskLovgivningTidslinje(jan2020)

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(
                "---      ",
                barn1,
                annenForeldersAktivitetsland = null,
                erAnnenForelderOmfattetAvNorskLovgivning = true,
            )
            .medKompetanse(
                "     ----",
                barn1,
                annenForeldersAktivitetsland = null,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )
            .byggKompetanser()

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder, emptyMap(), annenForelderOmfattetTidslinje)

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserUtenEøsPerioder() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = emptyMap<Aktør, Tidslinje<KombinertRegelverkResultat, Måned>>()

        val forventedeKompetanser = emptyList<Kompetanse>()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder, emptyMap())

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsEttBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val barnasRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020).kombinertSøkersResultatTidslinje(),
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS  SS--", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasRegelverkResultatTidslinjer,
            emptyMap(),
        )

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsToBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS--SSSS", barn1, barn2)
            .byggKompetanser()

        val barnasEgneRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            barn2.aktør to "EEEENNEEE".tilRegelverkResultatTidslinje(jan2020),
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS-   SS-", barn1, barn2)
            .medKompetanse("     S", barn1)
            .medKompetanse("   - ", barn2)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasEgneRegelverkResultatTidslinjer.mapValues { it.value.kombinertSøkersResultatTidslinje() },
            emptyMap(),
        ).sortedBy { it.fom }

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsForFlereBarn() {
        // "SSSSSSS", barn1
        // "SSSPPSS", barn2
        // "-SSSSSS", barn3

        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val barnasEgneRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            barn2.aktør to "EEE--NNNN".tilRegelverkResultatTidslinje(jan2020),
            barn3.aktør to "EEEEEEEEE".tilRegelverkResultatTidslinje(jan2020),
        )

        // SSS  SS--, barn1
        // SSS      , barn2
        // -SSSSSS--, barn3

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS      ", barn1, barn2, barn3)
            .medKompetanse("S        ", barn1, barn2)
            .medKompetanse("     SS--", barn1, barn3)
            .medKompetanse("-  SS    ", barn3)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasEgneRegelverkResultatTidslinjer.mapValues { it.value.kombinertSøkersResultatTidslinje() },
            emptyMap(),
        ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn med åpne regelverkstidslinjer`() {
        val kompetanser: List<Kompetanse> = emptyList()

        val barnasRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEEEEEEEE>".tilRegelverkResultatTidslinje(jan2020),
            barn2.aktør to "  EEEEEEEEE>".tilRegelverkResultatTidslinje(jan2020),
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("--", barn1)
            .medKompetanse("  ->", barn1, barn2)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasRegelverkResultatTidslinjer.mapValues { it.value.kombinertSøkersResultatTidslinje() },
            emptyMap(),
        ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn som ikke lenger har EØS-perioder`() {
        // "SSSSSSS", barn1
        // "SSSPPSS", barn2
        // "-SSSSSS", barn3

        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val barnasRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            barn2.aktør to "EEE--NNNN".tilRegelverkResultatTidslinje(jan2020),
            barn3.aktør to "NNNN-----".tilRegelverkResultatTidslinje(jan2020),
        )

        // SSS  SS--, barn1
        // SSS      , barn2

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS      ", barn1, barn2)
            .medKompetanse("     SS--", barn1)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasRegelverkResultatTidslinjer.mapValues { it.value.kombinertSøkersResultatTidslinje() },
            emptyMap(),
        ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser mot eøs for to barn, der ett barn har etterbetaling 3 år`() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS--SSSS", barn1, barn2)
            .byggKompetanser()

        val barnasRegelverkResultatTidslinjer = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            barn2.aktør to "EEEENNEEE".tilRegelverkResultatTidslinje(jan2020),
        )

        val barnasHarEtterbetaling3År = mapOf(
            barn1.aktør to "TTT      ".somBoolskTidslinje(jan2020),
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("      SS-", barn1, barn2)
            .medKompetanse("     S", barn1)
            .medKompetanse("SS-- ", barn2)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(
            kompetanser,
            barnasRegelverkResultatTidslinjer.mapValues { it.value.kombinertSøkersResultatTidslinje() },
            barnasHarEtterbetaling3År,
        ).sortedBy { it.fom }

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }
}

private fun Tidslinje<RegelverkResultat, Måned>.kombinertSøkersResultatTidslinje(
    søkersTidslinje: Tidslinje<RegelverkResultat, Måned>? = null,
): Tidslinje<KombinertRegelverkResultat, Måned> {
    return this.kombinerMed(søkersTidslinje ?: this) { barnetsResultat: RegelverkResultat?, søkersResultat: RegelverkResultat? ->
        KombinertRegelverkResultat(barnetsResultat, søkersResultat)
    }
}
