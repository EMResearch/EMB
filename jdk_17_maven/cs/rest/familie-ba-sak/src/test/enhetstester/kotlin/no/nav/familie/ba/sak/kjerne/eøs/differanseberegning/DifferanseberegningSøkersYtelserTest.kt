package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.barn
import no.nav.familie.ba.sak.kjerne.eøs.util.født
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DifferanseberegningSøkersYtelserTest {

    @Test
    fun `skal håndtere tre barn og utvidet barnetrygd og småbarnstillegg, der alle barna har underskudd i differanseberegning`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jan(2017))
            //              |1 stk <3 år|2 stk <3 år|3 stk <3 år|2 stk <3 år|1 stk <3 år|
            //              |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medKompetanse("PPPPPPSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPSSS", barn1)
            .medKompetanse("            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPPPPSSSSSSSSSSSSSSSSSSSSSSSS>", barn2)
            .medKompetanse("                        SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS>", barn3)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$") { 1000 }
            .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                                   $$$") { 1000 }
            .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                          $$$$$$") { 1000 }
            .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
            .forPersoner(barn3)
            .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>", 100, { 1000 }, { -700 }) { 0 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        val forventet = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medUtvidet("$$$$$$") { 1000 }
            .medUtvidet("      $$$$$$", { 1000 }, { 300 }) { 300 }
            .medUtvidet("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medUtvidet("                                          $$$$$$") { 1000 }
            .medUtvidet("                                                $$$", { 1000 }, { 0 }) { 0 }
            .medUtvidet("                                                   $$$") { 1000 }
            .medUtvidet("                                                      $$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("            $$$$$$$$$$$$", { 1000 }, { 600 }) { 600 }
            .medSmåbarn("                        $$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("                                    $$$$$$", { 1000 }, { 267 }) { 267 }
            .medSmåbarn("                                          $$$$$$") { 1000 }
            .medSmåbarn("                                                $$$", { 1000 }, { 633 }) { 633 }
            .medSmåbarn("                                                   $$$", { 1000 }, { 300 }) { 300 }
            .medSmåbarn("                                                      $$$", { 1000 }, { 633 }) { 633 }
            .medSmåbarn("                                                         $$$", { 1000 }, { 800 }) { 800 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$") { 1000 }
            .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                                   $$$") { 1000 }
            .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                          $$$$$$") { 1000 }
            .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
            .forPersoner(barn3)
            .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>", 100, { 1000 }, { -700 }) { 0 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `differanseberegnet ordinær barnetrygd uten at søker har ytelser, skal gi uendrete andeler for barna`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jan(2017))
            //              |1 stk <3 år|2 stk <3 år|3 stk <3 år|2 stk <3 år|1 stk <3 år|
            //              |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medKompetanse("PPPPPPSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPSSS", barn1)
            .medKompetanse("            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPPPPSSSSSSSSSSSSSSSSSSSSSSSS>", barn2)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .forPersoner(barn1)
            .medOrdinær("$$$$$$") { 1000 }
            .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                                   $$$") { 1000 }
            .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
            .medOrdinær("                                          $$$$$$") { 1000 }
            .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        assertEquals(tilkjentYtelse.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Ingen differranseberegning skal gi uendrete andeler`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        val kompetanser = emptyList<Kompetanse>()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        assertEquals(tilkjentYtelse.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Tom tilkjent ytelse og ingen barn skal ikke gi feil`() {
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val nyeAndeler = tilkjentYtelse.andelerTilkjentYtelse
            .differanseberegnSøkersYtelser(emptyList(), emptyList())

        assertEquals(emptyList<AndelTilkjentYtelse>(), nyeAndeler)
    }

    @Test
    fun `Søkers andel som har hatt differanseberegning, men ikke skal ha det lenger, skal fjerne differanseberegningen og slå sammen perioder som med sikkerhet kan slås sammen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser = emptyList<Kompetanse>()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medUtvidet("$$$$$$") { 1000 }
            .medUtvidet("      $$$$$$", { 1000 }, { 300 }) { 300 }
            .medUtvidet("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medUtvidet("                                          $$$$$$") { 1000 }
            .medUtvidet("                                                $$$", { 1000 }, { 0 }) { 0 }
            .medUtvidet("                                                   $$$") { 1000 }
            .medUtvidet("                                                      $$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("            $$$$$$$$$$$$", { 1000 }, { 600 }) { 600 }
            .medSmåbarn("                        $$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("                                    $$$$$$", { 1000 }, { 267 }) { 267 }
            .medSmåbarn("                                          $$$$$$") { 1000 }
            .medSmåbarn("                                                $$$", { 1000 }, { 633 }) { 633 }
            .medSmåbarn("                                                   $$$") { 1000 }
            .medSmåbarn("                                                      $$$", { 1000 }, { 633 }) { 633 }
            .medSmåbarn("                                                         $$$", { 1000 }, { 800 }) { 800 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
            .forPersoner(barn3)
            .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        // Dette er litt trist. Men selv om andelene er identiske, kan de ikke slås sammen fordi
        // de er til forveksling like som andeler som har en funksjonell årsak til å være splittet
        // Påfølgende andeler som begge har differanseberegning, KAN slås sammen
        val forventet = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
            .medUtvidet("$$$$$$") { 1000 }
            .medUtvidet("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .medUtvidet("                                          $$$$$$") { 1000 }
            .medUtvidet("                                                $$$") { 1000 }
            .medUtvidet("                                                   $$$") { 1000 }
            .medUtvidet("                                                      $$$$$$$$$$") { 1000 }
            .medSmåbarn("$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("                                          $$$$$$") { 1000 }
            .medSmåbarn("                                                $$$") { 1000 }
            .medSmåbarn("                                                   $$$") { 1000 }
            .medSmåbarn("                                                      $$$$$$") { 1000 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn2)
            .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
            .forPersoner(barn3)
            .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Søkers andel som har differanseberegning, men underskuddet reduseres, skal få oppdatert differanseberegningen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barna = listOf(barn1)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jan(2017))
            .medKompetanse("S>", barn1)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 300 }) { 300 }
            .forPersoner(barn1)
            .medOrdinær("$>", 100, { 1000 }, { -650 }) { 0 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        val forventet = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 350 }) { 350 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn1)
            .medOrdinær("$>", 100, { 1000 }, { -650 }) { 0 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Skal tåle perioder der underskuddet på differanseberegning er større enn alle tilkjente ytelser`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barna = listOf(barn1)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jan(2017))
            .medKompetanse("S>", barn1)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
            .forPersoner(barn1)
            .medOrdinær("$>", 100, { 1000 }, { -2650 }) { 0 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        val forventet = TilkjentYtelseBuilder(jan(2017), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
            .forPersoner(barn1)
            .medOrdinær("$>", 100, { 1000 }, { -2650 }) { 0 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `skal illustrere avrundingssproblematikk, der søker tjener`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jul(2020))
            .medKompetanse("SSSSSS", barn1, barn2, barn3)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jul(2020), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$") { 1054 }
            .forPersoner(barn1)
            .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
            .forPersoner(barn2, barn3)
            .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        val forventet = TilkjentYtelseBuilder(jul(2020), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$", { 1054 }, { 703 }) { 703 } // Egentlig 702,67
            .forPersoner(barn1)
            .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
            .forPersoner(barn2, barn3)
            .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `skal illustrere avrundingssproblematikk, der søker taper`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser = KompetanseBuilder(jul(2020))
            .medKompetanse("SSSSSS", barn1, barn2, barn3)
            .byggKompetanser()

        val tilkjentYtelse = TilkjentYtelseBuilder(jul(2020), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$") { 1054 }
            .forPersoner(barn1, barn2)
            .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
            .forPersoner(barn3)
            .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
            .bygg()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser)

        val forventet = TilkjentYtelseBuilder(jul(2020), behandling)
            .forPersoner(søker)
            .medUtvidet("$$$$$$", { 1054 }, { 351 }) { 351 } // Egentlig 351,33
            .forPersoner(barn1, barn2)
            .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
            .forPersoner(barn3)
            .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
            .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }
}

private fun Collection<AndelTilkjentYtelse>.sortert() =
    this.sortedWith(compareBy({ it.aktør.aktørId }, { it.type }, { it.stønadFom }))
