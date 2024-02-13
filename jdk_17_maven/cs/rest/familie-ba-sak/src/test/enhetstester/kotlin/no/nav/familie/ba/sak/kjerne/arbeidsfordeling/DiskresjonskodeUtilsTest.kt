package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiskresjonskodeUtilsTest {
    val personUtenDiskresjonskode = IdentMedAdressebeskyttelse(
        ident = "IDENT_UTEN",
        adressebeskyttelsegradering = null,
    )
    val personFortrolig = IdentMedAdressebeskyttelse(
        ident = "IDENT_FORTROLIG",
        adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG,
    ) // Kode 7
    val personStrengtFortrolig = IdentMedAdressebeskyttelse(
        ident = "IDENT_STRENGT_FORTROLIG",
        adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
    ) // Kode 6

    @Test
    fun `ingen har adressebeskyttelse - skal gi null`() {
        assertEquals(
            null,
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personUtenDiskresjonskode,
                    personUtenDiskresjonskode,
                ),
            ),
        )
    }

    @Test
    fun `en har adressebeskyttelse STRENGT_FORTROLIG, en har adressebeskyttelse FORTROLIG, en har ingen adressebeskyttelse - skal gi adressebeskyttelse STRENGT_FORTROLIG`() {
        assertEquals(
            "IDENT_STRENGT_FORTROLIG",
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personFortrolig,
                    personStrengtFortrolig,
                    personUtenDiskresjonskode,
                ),
            ),
        )
    }

    @Test
    fun `en har adressebeskyttelse FORTROLIG, en har ingen adressebeskyttelse - skal gi adressebeskyttelse FORTROLIG`() {
        assertEquals(
            "IDENT_FORTROLIG",
            finnPersonMedStrengesteAdressebeskyttelse(
                listOf(
                    personUtenDiskresjonskode,
                    personFortrolig,
                ),
            ),
        )
    }

    @Test
    fun `tom liste - skal gi null`() {
        assertEquals(
            null,
            finnPersonMedStrengesteAdressebeskyttelse(listOf()),
        )
    }
}
