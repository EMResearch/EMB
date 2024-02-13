package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

data class PdlHentPersonResponse(val person: PdlPersonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>,
    val foedsel: List<PdlFødselsDato>,
    val navn: List<PdlNavn> = emptyList(),
    val kjoenn: List<PdlKjoenn> = emptyList(),
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val sivilstand: List<Sivilstand> = emptyList(),
    val bostedsadresse: List<Bostedsadresse>,
    val opphold: List<Opphold> = emptyList(),
    val statsborgerskap: List<Statsborgerskap> = emptyList(),
    val doedsfall: List<Doedsfall> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<PdlKontaktinformasjonForDødsbo> = emptyList(),
) {
    fun validerOmPersonKanBehandlesIFagsystem() {
        if (foedsel.isEmpty()) throw PdlPersonKanIkkeBehandlesIFagsystem("mangler fødselsdato")
        if (folkeregisteridentifikator.firstOrNull()?.status == FolkeregisteridentifikatorStatus.OPPHOERT) {
            throw PdlPersonKanIkkeBehandlesIFagsystem(
                "er opphørt",
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFolkeregisteridentifikator(
    val identifikasjonsnummer: String?,
    val status: FolkeregisteridentifikatorStatus,
    val type: FolkeregisteridentifikatorType?,
)

enum class FolkeregisteridentifikatorStatus { I_BRUK, OPPHOERT }
enum class FolkeregisteridentifikatorType { FNR, DNR }

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(val foedselsdato: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
) {

    fun fulltNavn(): String {
        return when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(val kjoenn: Kjønn)
