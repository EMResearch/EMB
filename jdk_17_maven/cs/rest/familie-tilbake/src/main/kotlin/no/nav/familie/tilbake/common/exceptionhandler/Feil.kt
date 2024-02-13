package no.nav.familie.tilbake.common.exceptionhandler

import org.springframework.http.HttpStatus

data class ApiFeil(val feil: String, val httpStatus: HttpStatus) : RuntimeException()

class Feil(
    message: String,
    val frontendFeilmelding: String? = null,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable) {

    constructor(message: String, throwable: Throwable?, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR) :
        this(message, null, httpStatus, throwable)
}

class ManglerTilgang(val melding: String) : RuntimeException(melding)

class UgyldigKravgrunnlagFeil(val melding: String) : RuntimeException(melding)

class UkjentravgrunnlagFeil(val melding: String) : RuntimeException(melding)

class UgyldigStatusmeldingFeil(val melding: String) : RuntimeException(melding)

class SperretKravgrunnlagFeil(val melding: String) : IntegrasjonException(melding)

class KravgrunnlagIkkeFunnetFeil(val melding: String) : IntegrasjonException(melding)
