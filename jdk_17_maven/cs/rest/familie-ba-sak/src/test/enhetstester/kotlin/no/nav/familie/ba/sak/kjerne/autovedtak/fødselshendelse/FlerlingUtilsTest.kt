package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlerlingUtilsTest {

    @Test
    fun `Skal behandle 1 barn når mor ikke har andre barn`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barn)),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
            ),
            barnaSomHarBlittBehandlet = emptyList(),
        )

        assertEquals(1, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
    }

    @Test
    fun `Skal behandle 1 barn når mor har andre barn`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barn)),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now().minusYears(2),
                ),
            ),
            barnaSomHarBlittBehandlet = listOf(barn2),
        )

        assertEquals(1, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
    }

    @Test
    fun `Skal behandle 0 barn når mor har tvillinger og de allerede er behandlet`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, alleBarnSomKanBehandles) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barn2)),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now().minusYears(2),
                ),
            ),
            barnaSomHarBlittBehandlet = listOf(barn, barn2),
        )

        assertEquals(0, barnSomSkalBehandlesForMor.size)
        assertEquals(1, alleBarnSomKanBehandles.size)
    }

    @Test
    fun `Skal behandle 2 barn når hendelse inneholder flere barn og mor har ikke andre barn selv om barn ikke er definert som flerling`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = morsIdent,
                barnasIdenter = listOf(barn, barn2),
            ),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now().minusYears(2),
                ),
            ),
            barnaSomHarBlittBehandlet = emptyList(),
        )

        assertEquals(2, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn2))
    }

    @Test
    fun `Skal behandle 2 barn når mor har fått tvilling på samme dag og hendelse inneholder 1 barn`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = morsIdent,
                barnasIdenter = listOf(barn),
            ),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
            ),
            barnaSomHarBlittBehandlet = emptyList(),
        )

        assertEquals(2, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn2))
    }

    @Test
    fun `Skal behandle 4 barn når mor har fått firlinger på samme dag og hendelse inneholder 1 barn`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val barn3 = randomFnr()
        val barn4 = randomFnr()
        val barn5 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = morsIdent,
                barnasIdenter = listOf(barn),
            ),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn3),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn4),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
            ),
            barnaSomHarBlittBehandlet = listOf(barn5),
        )

        assertEquals(4, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn2))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn3))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn4))
    }

    @Test
    fun `Skal behandle 2 barn når mor har fått tvilling med 1 dag mellomrom og hendelse inneholder 1 barn født dagen etter tvilling`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = morsIdent,
                barnasIdenter = listOf(barn),
            ),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now().minusDays(1),
                ),
            ),
            barnaSomHarBlittBehandlet = emptyList(),
        )

        assertEquals(2, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn2))
    }

    @Test
    fun `Skal behandle 2 barn når mor har fått tvilling med 1 dag mellomrom og hendelse inneholder 1 barn født dagen før tvilling`() {
        val morsIdent = randomFnr()
        val barn = randomFnr()
        val barn2 = randomFnr()
        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = morsIdent,
                barnasIdenter = listOf(barn),
            ),
            barnaTilMor = listOf(
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now(),
                ),
                ForelderBarnRelasjon(
                    aktør = tilAktør(barn2),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    fødselsdato = LocalDate.now().plusDays(1),
                ),
            ),
            barnaSomHarBlittBehandlet = emptyList(),
        )

        assertEquals(2, barnSomSkalBehandlesForMor.size)
        assertTrue(barnSomSkalBehandlesForMor.contains(barn))
        assertTrue(barnSomSkalBehandlesForMor.contains(barn2))
    }

    @Test
    fun `Skal lage oppgave fordi barnet i hendelse ikke behandles på åpen behandling`() {
        val barn = randomAktør()
        val barn2 = randomAktør()

        assertFalse(
            barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                barnaPåHendelse = listOf(barn),
                barnaPåÅpenBehandling = listOf(barn2),
            ),
        )
    }

    @Test
    fun `Skal ignorere hendelse fordi barnet i hendelse behandles på åpen behandling`() {
        val barn = randomAktør()
        val barn2 = randomAktør()

        assertTrue(
            barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                barnaPåHendelse = listOf(barn),
                barnaPåÅpenBehandling = listOf(barn, barn2),
            ),
        )
    }

    @Test
    fun `Skal ignorere hendelse fordi barna i hendelse behandles på åpen behandling`() {
        val barn = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()

        assertTrue(
            barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                barnaPåHendelse = listOf(barn, barn2),
                barnaPåÅpenBehandling = listOf(barn, barn2, barn3),
            ),
        )
    }

    @Test
    fun `Skal lage oppgave fordi kun 1 av barna i hendelse behandles på åpen behandling`() {
        val barn = randomAktør()
        val barn2 = randomAktør()
        val barn3 = randomAktør()

        assertFalse(
            barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                barnaPåHendelse = listOf(barn, barn2),
                barnaPåÅpenBehandling = listOf(barn, barn3),
            ),
        )
    }
}
