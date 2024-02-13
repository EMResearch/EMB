package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.vurderOmPersonerBorSammen
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BostedsadresseTest {

    @Test
    fun `Skal adresse med mest nylig fom-dato`() {
        val adresse = GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        )
        val adresseMedNullFom = adresse.copy().apply { periode = DatoIntervallEntitet(fom = null) }
        val adresseMedEldreDato = adresse.copy().apply { periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3)) }
        val adresseMedNyereDato = adresse.copy().apply { periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1)) }
        val adresserTilSortering = mutableListOf<GrBostedsadresse>(adresseMedEldreDato, adresseMedNyereDato, adresseMedNullFom)
        assertEquals(adresseMedNyereDato, adresserTilSortering.sisteAdresse())
    }

    @Test
    fun `Skal returnere adresse uten datoer når dette er eneste`() {
        val adresse = GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        ).apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        assertEquals(adresse, mutableListOf(adresse).sisteAdresse())
    }

    @Test
    fun `Skal kaste feil hvis det finnes flere adresser uten datoer`() {
        val adresse1 = GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        ).apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        val adresse2 = GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        ).apply {
            periode = DatoIntervallEntitet(fom = null)
        } as GrBostedsadresse
        assertThrows<Feil> { mutableListOf(adresse1, adresse2).sisteAdresse() }
    }

    @Test
    fun `Skal returnere at personer bor sammen når begge kun har felles adresse`() {
        val p1 = lagPerson()
        val p2 = lagPerson()
        val fellesAdresse = Bostedsadresse(
            angittFlyttedato = LocalDate.parse("2020-07-13"),
            gyldigTilOgMed = null,
            matrikkeladresse = Matrikkeladresse(
                matrikkelId = 123L,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
            ),
        )
        val p1Adresser = listOf(
            GrBostedsadresse.fraBostedsadresse(
                person = p1,
                bostedsadresse = fellesAdresse,
            ),
        )
        val p2Adresser = listOf(
            GrBostedsadresse.fraBostedsadresse(person = p2, bostedsadresse = fellesAdresse),
        )
        Assertions.assertTrue(vurderOmPersonerBorSammen(p1Adresser, p2Adresser))
    }

    @Test
    fun `Skal returnere at personer bor sammen når en av personene har flere adresser`() {
        val p1 = lagPerson()
        val p2 = lagPerson()
        val fellesAdresse = Bostedsadresse(
            angittFlyttedato = LocalDate.parse("2020-07-13"),
            gyldigTilOgMed = null,
            matrikkeladresse = Matrikkeladresse(
                matrikkelId = 123L,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
            ),
        )
        val p1Adresser = listOf(
            GrBostedsadresse.fraBostedsadresse(
                person = p1,
                bostedsadresse = fellesAdresse,
            ),
        )
        val p2Adresser = listOf(
            GrBostedsadresse.fraBostedsadresse(person = p2, bostedsadresse = fellesAdresse),
            GrBostedsadresse.fraBostedsadresse(
                person = p2,
                bostedsadresse = Bostedsadresse(
                    angittFlyttedato = LocalDate.parse("2021-08-09"),
                    gyldigTilOgMed = null,
                    matrikkeladresse = Matrikkeladresse(
                        matrikkelId = 145L,
                        bruksenhetsnummer = "H402",
                        tilleggsnavn = "ekstra",
                        postnummer = "0333",
                        kommunenummer = "3456",
                    ),
                ),
            ),
        )
        Assertions.assertTrue(vurderOmPersonerBorSammen(p1Adresser.filtrerGjeldendeNå(), p2Adresser.filtrerGjeldendeNå()))
    }
}
