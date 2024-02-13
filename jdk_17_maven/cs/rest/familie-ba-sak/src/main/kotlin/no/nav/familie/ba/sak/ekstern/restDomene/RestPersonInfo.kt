package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import java.time.LocalDate

data class RestPersonInfo(
    val personIdent: String,
    var fødselsdato: LocalDate? = null,
    val navn: String? = null,
    val kjønn: Kjønn? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    var harTilgang: Boolean = true,
    val forelderBarnRelasjon: List<RestForelderBarnRelasjon> = emptyList(),
    val forelderBarnRelasjonMaskert: List<RestForelderBarnRelasjonnMaskert> = emptyList(),
    val kommunenummer: String = "ukjent",
    val dødsfallDato: String? = null,
    val bostedsadresse: RestBostedsadresse? = null,
)

data class RestForelderBarnRelasjon(
    val personIdent: String,
    val relasjonRolle: FORELDERBARNRELASJONROLLE,
    val navn: String,
    val fødselsdato: LocalDate?,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
)

data class RestForelderBarnRelasjonnMaskert(
    val relasjonRolle: FORELDERBARNRELASJONROLLE,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
)

data class RestBostedsadresse(
    val adresse: String?,
    val postnummer: String,
)

private fun ForelderBarnRelasjonMaskert.tilRestForelderBarnRelasjonMaskert() = RestForelderBarnRelasjonnMaskert(
    relasjonRolle = this.relasjonsrolle,
    adressebeskyttelseGradering = this.adressebeskyttelseGradering,
)

private fun ForelderBarnRelasjon.tilRestForelderBarnRelasjon() = RestForelderBarnRelasjon(
    personIdent = this.aktør.aktivFødselsnummer(),
    relasjonRolle = this.relasjonsrolle,
    navn = this.navn ?: "",
    fødselsdato = this.fødselsdato,
    adressebeskyttelseGradering = this.adressebeskyttelseGradering,

)

fun PersonInfo.tilRestPersonInfo(personIdent: String): RestPersonInfo {
    val bostedsadresse =
        this.bostedsadresser.filter { it.angittFlyttedato != null }.maxByOrNull { it.angittFlyttedato!! }
    val kommunenummer: String = when {
        bostedsadresse == null -> null
        bostedsadresse.vegadresse != null -> bostedsadresse.vegadresse?.kommunenummer
        bostedsadresse.matrikkeladresse != null -> bostedsadresse.matrikkeladresse?.kommunenummer
        bostedsadresse.ukjentBosted != null -> null
        else -> null
    } ?: "ukjent"

    val dødsfallDato = if (this.dødsfall != null && this.dødsfall.erDød) this.dødsfall.dødsdato else null

    return RestPersonInfo(
        personIdent = personIdent,
        fødselsdato = this.fødselsdato,
        navn = this.navn,
        kjønn = this.kjønn,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering,
        forelderBarnRelasjon = this.forelderBarnRelasjon.map { it.tilRestForelderBarnRelasjon() },
        forelderBarnRelasjonMaskert = this.forelderBarnRelasjonMaskert.map { it.tilRestForelderBarnRelasjonMaskert() },
        kommunenummer = kommunenummer,
        dødsfallDato = dødsfallDato,
    )
}

fun PersonInfo.tilRestPersonInfoMedNavnOgAdresse(personIdent: String): RestPersonInfo {
    val bostedsadresse = this.bostedsadresser.singleOrNull() // det skal kun være en bostedsadresse i PersonInfo uten historikk
    val postnummer = bostedsadresse?.vegadresse?.postnummer ?: bostedsadresse?.matrikkeladresse?.postnummer
    return RestPersonInfo(
        personIdent = personIdent,
        fødselsdato = this.fødselsdato,
        navn = this.navn,
        bostedsadresse = when (postnummer) {
            null -> null
            else -> RestBostedsadresse(
                adresse = bostedsadresse?.vegadresse?.adressenavn?.plus(" ${bostedsadresse.vegadresse?.husnummer ?: ""}")?.trim(),
                postnummer = postnummer,
            )
        },
    )
}
