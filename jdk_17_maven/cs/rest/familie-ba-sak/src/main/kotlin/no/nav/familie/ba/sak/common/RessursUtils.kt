package no.nav.familie.ba.sak.common

import io.sentry.Sentry
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursUtils {

    private val logger = LoggerFactory.getLogger(RessursUtils::class.java)

    fun <T> unauthorized(errorMessage: String): ResponseEntity<Ressurs<T>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Ressurs.failure(errorMessage))

    fun <T> badRequest(errorMessage: String, throwable: Throwable): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.BAD_REQUEST, errorMessage, throwable)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
        ikkeTilgangResponse(errorMessage)

    fun <T> illegalState(errorMessage: String, throwable: Throwable): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, throwable)

    fun <T> funksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<T>> = funksjonellErrorResponse(
        funksjonellFeil,
    )

    fun <T> frontendFeil(feil: Feil, throwable: Throwable?): ResponseEntity<Ressurs<T>> =
        frontendErrorResponse(feil, throwable)

    fun <T> ok(data: T): ResponseEntity<Ressurs<T>> = ResponseEntity.ok(Ressurs.success(data))

    fun <T> rolleTilgangResponse(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<T>> {
        secureLogger.warn(
            "En håndtert tilgangsfeil har oppstått - ${rolleTilgangskontrollFeil.frontendFeilmelding}",
            rolleTilgangskontrollFeil,
        )
        logger.warn("En håndtert tilgangsfeil har oppstått - ${rolleTilgangskontrollFeil.melding}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                Ressurs.ikkeTilgang<T>(rolleTilgangskontrollFeil.melding)
                    .copy(frontendFeilmelding = rolleTilgangskontrollFeil.frontendFeilmelding.ifBlank { "Mangler tilgang" }),
            )
    }

    private fun <T> errorResponse(
        httpStatus: HttpStatus,
        errorMessage: String,
        throwable: Throwable,
    ): ResponseEntity<Ressurs<T>> {
        val className = "[${throwable::class.java.name}] "

        secureLogger.warn("$className En feil har oppstått: $errorMessage", throwable)
        logger.warn("$className En feil har oppstått. Se securelogs for detaljer.")

        Sentry.captureException(throwable)
        return ResponseEntity.status(httpStatus).body(Ressurs.failure(errorMessage))
    }

    private fun <T> ikkeTilgangResponse(
        errorMessage: String,
    ): ResponseEntity<Ressurs<T>> {
        secureLogger.warn("Saksbehandler har ikke tilgang: $errorMessage")
        logger.warn("Saksbehandler har ikke tilgang. Se securelogs for detaljer.")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Ressurs.ikkeTilgang(errorMessage))
    }

    private fun <T> frontendErrorResponse(feil: Feil, throwable: Throwable?): ResponseEntity<Ressurs<T>> {
        val className = if (throwable != null) "[${throwable::class.java.name}] " else ""

        secureLogger.info(
            "$className En håndtert feil har oppstått(${feil.httpStatus}): " +
                "${feil.message}, ${feil.frontendFeilmelding}",
            feil,
        )
        logger.warn("$className En håndtert feil har oppstått(${feil.httpStatus}): ${feil.message} ", feil)

        Sentry.captureException(feil)
        return ResponseEntity.status(feil.httpStatus).body(
            Ressurs.failure(
                frontendFeilmelding = feil.frontendFeilmelding,
                errorMessage = feil.message.toString(),
            ),
        )
    }

    private fun <T> funksjonellErrorResponse(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<T>> {
        val className =
            if (funksjonellFeil.throwable != null) "[${funksjonellFeil.throwable!!::class.java.name}] " else ""

        logger.info("$className En funksjonell feil har oppstått(${funksjonellFeil.httpStatus}): ${funksjonellFeil.message} ")

        return ResponseEntity.status(funksjonellFeil.httpStatus).body(
            Ressurs.funksjonellFeil(
                frontendFeilmelding = funksjonellFeil.frontendFeilmelding,
                melding = funksjonellFeil.melding,
            ),
        )
    }
}
