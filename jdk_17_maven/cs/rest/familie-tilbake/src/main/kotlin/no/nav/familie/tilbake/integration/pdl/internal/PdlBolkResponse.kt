package no.nav.familie.tilbake.integration.pdl.internal

data class PdlBolkResponse<T>(val data: PersonBolk<T>?, val errors: List<PdlError>?, val extensions: PdlExtensions?) {

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
    fun harAdvarsel(): Boolean {
        return !extensions?.warnings.isNullOrEmpty()
    }
}

data class PersonBolk<T>(val personBolk: List<PersonDataBolk<T>>)

data class PersonDataBolk<T>(val ident: String, val code: String, val person: T?)
