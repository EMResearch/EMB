package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

open class PdlBaseResponse<T>(
    val data: T,
    open val errors: List<PdlError>?,
    open val extensions: PdlExtensions?,
) {

    fun harFeil(): Boolean {
        return errors != null && errors!!.isNotEmpty()
    }
    fun harAdvarsel(): Boolean {
        return !extensions?.warnings.isNullOrEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}

data class PdlExtensions(val warnings: List<PdlWarning>?)

data class PdlWarning(val details: Any?, val id: String?, val message: String?, val query: String?)
