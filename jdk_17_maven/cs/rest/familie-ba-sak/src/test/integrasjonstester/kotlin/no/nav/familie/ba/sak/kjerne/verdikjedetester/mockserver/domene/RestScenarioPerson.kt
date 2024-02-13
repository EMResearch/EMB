package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFolkeregisteridentifikator
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate
import java.time.Month
import java.time.Period

data class RestScenarioPerson(
    val ident: String? = null, // Settes av mock-server
    val aktørId: String? = null, // Settes av mock-server
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(), // Settes av mock-server
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator> = emptyList(), // Settes av mock-server
    val fødselsdato: String, // yyyy-mm-dd
    val fornavn: String,
    val etternavn: String,
    val infotrygdSaker: InfotrygdSøkResponse<Sak>? = null,
    val statsborgerskap: List<Statsborgerskap> = listOf(
        Statsborgerskap(
            land = "NOR",
            gyldigFraOgMed = LocalDate.parse(fødselsdato),
            bekreftelsesdato = LocalDate.parse(fødselsdato),
            gyldigTilOgMed = null,
        ),
    ),
    val bostedsadresser: List<Bostedsadresse> = defaultBostedsadresseHistorikk,
) {

    val navn = "$fornavn $etternavn"

    val alder = Period.between(LocalDate.parse(fødselsdato), LocalDate.now()).years
}

val defaultBostedsadresseHistorikk = mutableListOf(
    Bostedsadresse(
        angittFlyttedato = LocalDate.now().minusDays(15),
        gyldigTilOgMed = null,
        matrikkeladresse = Matrikkeladresse(
            matrikkelId = 123L,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        ),
    ),
    Bostedsadresse(
        angittFlyttedato = LocalDate.of(2018, Month.JANUARY, 1),
        gyldigTilOgMed = LocalDate.now().minusDays(16),
        matrikkeladresse = Matrikkeladresse(
            matrikkelId = 123L,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        ),
    ),
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: String,
)
