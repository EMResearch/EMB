package no.nav.familie.tilbake.integration.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class PdlHentPersonResponse<T>(
    val data: T,
    override val errors: List<PdlError>?,
    override val extensions: PdlExtensions?,
) : PdlBaseResponse(errors, extensions)

data class PdlPerson(val person: PdlPersonData?)

data class PdlPersonData(
    @JsonProperty("foedsel") val fødsel: List<PdlFødselsDato>,
    val navn: List<PdlNavn>,
    @JsonProperty("kjoenn") val kjønn: List<PdlKjønn>,
    @JsonProperty("doedsfall") val dødsfall: List<PdlDødsfall> = emptyList(),
    @JsonProperty("folkeregisteridentifikator") val identer: List<PdlFolkeregisteridentifikator>,
)

data class PdlFødselsDato(@JsonProperty("foedselsdato") val fødselsdato: String?)

data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(val code: String?)

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

data class PdlKjønn(@JsonProperty("kjoenn") val kjønn: Kjønn)

enum class Kjønn {
    MANN,
    KVINNE,
    UKJENT,
}

data class PdlDødsfall(@JsonProperty("doedsdato") val dødsdato: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFolkeregisteridentifikator(
    val identifikasjonsnummer: String?,
    val status: FolkeregisteridentifikatorStatus,
    val type: FolkeregisteridentifikatorType?,
)

enum class FolkeregisteridentifikatorStatus { I_BRUK, OPPHOERT }
enum class FolkeregisteridentifikatorType { FNR, DNR }
