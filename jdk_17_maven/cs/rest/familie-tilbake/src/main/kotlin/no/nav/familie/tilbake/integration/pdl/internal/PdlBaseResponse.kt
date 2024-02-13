package no.nav.familie.tilbake.integration.pdl.internal

open class PdlBaseResponse(open val errors: List<PdlError>?, open val extensions: PdlExtensions?) {

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

data class PdlExtensions(val warnings: List<PdlWarning>?)
data class PdlWarning(val details: Any?, val id: String?, val message: String?, val query: String?)
