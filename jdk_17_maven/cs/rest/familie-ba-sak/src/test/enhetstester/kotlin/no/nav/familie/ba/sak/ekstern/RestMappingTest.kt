package no.nav.familie.ba.sak.ekstern

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.ekstern.restDomene.fyllInnTomDatoer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.fregManglendeFlytteDato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RestMappingTest {

    @Test
    fun `Manglende angitt flyttedato fra freg mappes som manglende dato`() {
        val adresseUtenFlyttedato = GrVegadresse(
            matrikkelId = 1234,
            husnummer = "11",
            husbokstav = "B",
            bruksenhetsnummer = "H022",
            adressenavn = "Adressenavn",
            kommunenummer = "1232",
            tilleggsnavn = "noe",
            postnummer = "4322",
        )
            .apply { periode = DatoIntervallEntitet(fom = fregManglendeFlytteDato) }

        val flyttedato = LocalDate.of(2000, 1, 1)
        val adresseMedFlyttedato = GrVegadresse(
            matrikkelId = 1234,
            husnummer = "11",
            husbokstav = "B",
            bruksenhetsnummer = "H022",
            adressenavn = "Adressenavn",
            kommunenummer = "1232",
            tilleggsnavn = "noe",
            postnummer = "4322",
        )
            .apply { periode = DatoIntervallEntitet(fom = flyttedato) }

        assertEquals(null, adresseUtenFlyttedato.tilRestRegisteropplysning().fom)
        assertEquals(flyttedato, adresseMedFlyttedato.tilRestRegisteropplysning().fom)
    }

    @Test
    fun `Fyller ut og sorterer i rett rekkefølge`() {
        val fomA = LocalDate.of(2001, 1, 1)
        val fomB = LocalDate.of(2005, 1, 1)
        val tomB = LocalDate.of(2006, 1, 1)
        val fomC = LocalDate.of(2010, 1, 1)

        val manglerDatoer = RestRegisteropplysning(fom = null, tom = null, verdi = "")
        val tidligereUtenTom = RestRegisteropplysning(fom = fomA, tom = null, verdi = "")
        val tidligereMedTom = RestRegisteropplysning(fom = fomB, tom = tomB, verdi = "")
        val nåværende = RestRegisteropplysning(fom = fomC, tom = null, verdi = "")

        val utfylteOpplysninger =
            listOf(manglerDatoer, tidligereUtenTom, tidligereMedTom, nåværende).shuffled().fyllInnTomDatoer()

        assertEquals(null, utfylteOpplysninger[0].tom)
        assertEquals(fomB.minusDays(1), utfylteOpplysninger[1].tom)
        assertEquals(tomB, utfylteOpplysninger[2].tom)
        assertEquals(null, utfylteOpplysninger[3].tom)
    }

    @Test
    fun `Fyller ut tom-dato når denne mangler og det er påfølgende periode`() {
        val tidligereUtenTom = RestRegisteropplysning(fom = LocalDate.of(2001, 1, 1), tom = null, verdi = "")
        val nåværendeFom = LocalDate.of(2005, 1, 1)
        val nåværende = RestRegisteropplysning(fom = nåværendeFom, tom = null, verdi = "")
        val utfylteOpplysninger =
            listOf(tidligereUtenTom, nåværende).fyllInnTomDatoer()
        assertEquals(nåværendeFom.minusDays(1), utfylteOpplysninger[0].tom)
    }

    @Test
    fun `Fyller ikke ut tom-dato når det ikke finnes påfølgende perioder`() =
        assertEquals(null, listOf(RestRegisteropplysning(fom = null, tom = null, verdi = ""))[0].tom)

    @Test
    fun `Fyller ikke ut tom-dato når fom-dato er kjent`() =
        assertEquals(null, listOf(RestRegisteropplysning(fom = null, tom = null, verdi = ""))[0].tom)

    @Test
    fun `Fyller ikke ut tom-dato når denne er kjent, ved utvandring`() {
        val tomDato = LocalDate.of(2006, 1, 1)
        assertEquals(tomDato, listOf(RestRegisteropplysning(fom = LocalDate.of(2005, 1, 1), tom = tomDato, verdi = ""))[0].tom)
    }
}
