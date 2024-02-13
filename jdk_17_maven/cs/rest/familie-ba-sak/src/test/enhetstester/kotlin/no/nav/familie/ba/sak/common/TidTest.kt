package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class TidTest {

    @Test
    fun `skal finne siste dag i måneden før 2020-03-01`() {
        assertEquals(dato("2020-02-29"), dato("2020-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden før 2021-03-01`() {
        assertEquals(dato("2021-02-28"), dato("2021-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden forrige år`() {
        assertEquals(dato("2019-12-31"), dato("2020-01-15").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne første dag neste år`() {
        assertEquals(dato("2020-01-01"), dato("2019-12-03").førsteDagINesteMåned())
    }

    @Test
    fun `skal finne første dag i måneden etter skuddårsdagen`() {
        assertEquals(dato("2020-03-01"), dato("2020-02-29").førsteDagINesteMåned())
    }

    @Test
    fun `skal finne siste dag i inneværende måned 2020-03-01`() {
        assertEquals(dato("2020-03-31"), dato("2020-03-01").sisteDagIMåned())
    }

    @Test
    fun `skal finne siste dag i inneværende måned 2020-02-01 skuddår`() {
        assertEquals(dato("2020-02-29"), dato("2020-02-01").sisteDagIMåned())
    }

    @Test
    fun `skal returnere seneste dato av 2020-01-01 og 2019-01-01`() {
        assertEquals(dato("2020-01-01"), senesteDatoAv(dato("2020-01-01"), dato("2019-01-01")))
    }

    @Test
    fun `skal returnere true for dato som er senere enn`() {
        assertEquals(true, dato("2020-01-01").isSameOrAfter(dato("2019-01-01")))
    }

    @Test
    fun `skal returnere false for dato som er tidligere`() {
        assertEquals(false, dato("2019-01-01").isSameOrAfter(dato("2020-01-01")))
    }

    @Test
    fun `skal returnere true for dato som er lik`() {
        assertEquals(true, dato("2020-01-01").isSameOrAfter(dato("2020-01-01")))
    }

    @Test
    fun `skal returnere true dersom dato er dagen før en annen dato`() {
        assertTrue(dato("2020-04-30").erDagenFør(dato("2020-05-01")))
        assertFalse(dato("2020-04-30").erDagenFør(dato("2020-05-02")))
        assertFalse(dato("2020-05-01").erDagenFør(dato("2020-04-30")))
        assertFalse(dato("2020-04-30").erDagenFør(dato("2020-04-30")))
        assertFalse(dato("2020-04-30").erDagenFør(null))
    }

    @Test
    fun `dato i inneværende eller forrige måned`() {
        assertTrue(LocalDate.now().erFraInneværendeMåned())
        assertTrue(LocalDate.now().erFraInneværendeEllerForrigeMåned())
        assertFalse(LocalDate.now().minusMonths(1).erFraInneværendeMåned())
        assertTrue(LocalDate.now().minusMonths(1).erFraInneværendeEllerForrigeMåned())
        assertFalse(LocalDate.now().minusYears(1).erFraInneværendeMåned())
        assertFalse(LocalDate.now().minusYears(1).erFraInneværendeEllerForrigeMåned())
    }

    @Test
    fun `skal bestemme om periode er etterfølgende periode`() {
        val personAktørId = randomAktør()
        val behandling = lagBehandling()
        val resultat: Resultat = mockk()
        val vilkår: Vilkår = mockk(relaxed = true)
        val vilkårsvurdering = lagVilkårsvurdering(personAktørId, behandling, resultat)

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personAktørId,
        )

        val førsteVilkårResultat = VilkårResultat(
            personResultat = personResultat,
            resultat = resultat,
            vilkårType = vilkår,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = LocalDate.of(2020, 3, 25),
            begrunnelse = "",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
        )
        val etterfølgendeVilkårResultat = VilkårResultat(
            personResultat = personResultat,
            resultat = resultat,
            vilkårType = vilkår,
            periodeFom = LocalDate.of(2020, 3, 31),
            periodeTom = LocalDate.of(2020, 6, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
        )
        val ikkeEtterfølgendeVilkårResultat = VilkårResultat(
            personResultat = personResultat,
            resultat = resultat,
            vilkårType = vilkår,
            periodeFom = LocalDate.of(2020, 5, 1),
            periodeTom = LocalDate.of(2020, 6, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
        )

        assertTrue(førsteVilkårResultat.erEtterfølgendePeriode(etterfølgendeVilkårResultat))
        assertFalse(førsteVilkårResultat.erEtterfølgendePeriode(ikkeEtterfølgendeVilkårResultat))
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og bruke laveste fom og beholde tom fra periode 3`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2005, 9, 2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2015, 5, 20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014, 10, 1), LocalDate.of(2018, 5, 20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2018, 6, 1), null)

        val result = slåSammenOverlappendePerioder(listOf(periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
            .hasSize(3)
            .contains(periode1)
            .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2018, 5, 20)))
            .contains(currentPeriode)
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og bruke laveste fom og beholde tom fra periode 2`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2005, 9, 2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2018, 5, 20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014, 10, 1), LocalDate.of(2015, 5, 20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2018, 6, 1), null)

        val result = slåSammenOverlappendePerioder(listOf(periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
            .hasSize(3)
            .contains(periode1)
            .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2018, 5, 20)))
            .contains(currentPeriode)
    }

    @Test
    fun `skal slå sammen overlappende perioder med samme startdato`() {
        val result = slåSammenOverlappendePerioder(
            listOf(
                DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 2, 1)),
                DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 3, 1)),
                DatoIntervallEntitet(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 3, 1)),
                DatoIntervallEntitet(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 2, 1)),
            ),
        )

        Assertions.assertThat(result)
            .hasSize(2)
            .contains(DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 3, 1)))
            .contains(DatoIntervallEntitet(LocalDate.of(2005, 1, 1), LocalDate.of(2005, 3, 1)))
    }

    @Test
    fun `skal ikke slå sammen perioder som ligger inntil hverandre`() {
        val result = slåSammenOverlappendePerioder(
            listOf(
                DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 31)),
                DatoIntervallEntitet(LocalDate.of(2004, 2, 1), LocalDate.of(2004, 2, 28)),
            ),
        )

        Assertions.assertThat(result)
            .hasSize(2)
            .contains(DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 31)))
            .contains(DatoIntervallEntitet(LocalDate.of(2004, 2, 1), LocalDate.of(2004, 2, 28)))
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og videreføre null i tom`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2005, 9, 2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2015, 5, 20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014, 10, 1), LocalDate.of(2018, 5, 20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2008, 6, 1), null)

        val result = slåSammenOverlappendePerioder(listOf(periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
            .hasSize(2)
            .contains(periode1)
            .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), null))
    }

    @Test
    fun `skal slå sammen perioder til én periode hvor første periode har tom som null`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004, 1, 1), null)
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2015, 5, 20))

        val result = slåSammenOverlappendePerioder(listOf(periode1, periode2))
        Assertions.assertThat(result)
            .hasSize(1)
            .contains(periode1)
            .contains(DatoIntervallEntitet(LocalDate.of(2004, 1, 1), null))
    }

    @Test
    fun `hopp over perioder som ikke har fra-dato`() {
        val periode1 = DatoIntervallEntitet(null, null)
        val periode2 = DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 5))

        val result = slåSammenOverlappendePerioder(listOf(periode1, periode2))
        Assertions.assertThat(result)
            .hasSize(1)
    }

    @Test
    fun `det skal kun finnes en periode med tom = null  etter sammenslåing`() {
        val result = slåSammenOverlappendePerioder(
            listOf(
                DatoIntervallEntitet(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 1)),
                DatoIntervallEntitet(LocalDate.of(2005, 1, 1), null),
                DatoIntervallEntitet(LocalDate.of(2005, 5, 1), LocalDate.of(2005, 6, 1)),
                DatoIntervallEntitet(LocalDate.of(2006, 1, 1), null),
                DatoIntervallEntitet(LocalDate.of(2006, 5, 1), LocalDate.of(2006, 6, 1)),
            ),
        )
        Assertions.assertThat(result).hasSize(2)
        Assertions.assertThat(result.filter { it.tom != null }).hasSize(1)
    }

    @Test
    fun `formatering gir forventet resultat`() {
        assertEquals("31. desember 2020", dato("2020-12-31").tilDagMånedÅr())
        assertEquals("311220", dato("2020-12-31").tilddMMyy())
        assertEquals("31.12.20", dato("2020-12-31").tilKortString())
        assertEquals("desember 2020", dato("2020-12-31").tilMånedÅr())
    }

    @Test
    fun `sjekk for om to måned perioder helt eller delvis er overlappende`() {
        val jan2020_aug2020 = MånedPeriode(YearMonth.of(2020, 1), YearMonth.of(2020, 8))
        val jul2020_des2020 = MånedPeriode(YearMonth.of(2020, 7), YearMonth.of(2020, 12))
        val des2019_sep2021 = MånedPeriode(YearMonth.of(2019, 12), YearMonth.of(2020, 9))
        val jan2020 = MånedPeriode(YearMonth.of(2020, 1), YearMonth.of(2020, 1))
        val aug2020 = MånedPeriode(YearMonth.of(2020, 8), YearMonth.of(2020, 8))
        val des2019 = MånedPeriode(YearMonth.of(2019, 12), YearMonth.of(2019, 12))
        val sep2021 = MånedPeriode(YearMonth.of(2021, 9), YearMonth.of(2021, 9))

        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(jul2020_des2020))
        assertTrue(jul2020_des2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(des2019_sep2021))
        assertTrue(des2019_sep2021.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(jan2020))
        assertTrue(jan2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(aug2020))
        assertTrue(aug2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertFalse(jan2020_aug2020.overlapperHeltEllerDelvisMed(des2019))
        assertFalse(des2019.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertFalse(jan2020_aug2020.overlapperHeltEllerDelvisMed(sep2021))
        assertFalse(sep2021.overlapperHeltEllerDelvisMed(jan2020_aug2020))
    }

    @Test
    fun `sjekk for om to perioder helt eller delvis er overlappende`() {
        val jan2020_aug2020 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 1))
        val jul2020_des2020 = Periode(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 12, 1))
        val des2019_sep2021 = Periode(LocalDate.of(2019, 12, 1), LocalDate.of(2020, 9, 1))
        val jan2020 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))
        val aug2020 = Periode(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 1))
        val des2019 = Periode(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1))
        val sep2021 = Periode(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 9, 1))

        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(jul2020_des2020))
        assertTrue(jul2020_des2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(des2019_sep2021))
        assertTrue(des2019_sep2021.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(jan2020))
        assertTrue(jan2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertTrue(jan2020_aug2020.overlapperHeltEllerDelvisMed(aug2020))
        assertTrue(aug2020.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertFalse(jan2020_aug2020.overlapperHeltEllerDelvisMed(des2019))
        assertFalse(des2019.overlapperHeltEllerDelvisMed(jan2020_aug2020))
        assertFalse(jan2020_aug2020.overlapperHeltEllerDelvisMed(sep2021))
        assertFalse(sep2021.overlapperHeltEllerDelvisMed(jan2020_aug2020))
    }

    private fun dato(s: String): LocalDate {
        return LocalDate.parse(s)
    }
}
