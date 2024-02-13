package no.nav.familie.tilbake.common

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursUtils {

    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun <T> unauthorized(errorMessage: String): ResponseEntity<Ressurs<T>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Ressurs.failure(errorMessage))

    fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.NOT_FOUND, errorMessage, null)

    fun <T> badRequest(errorMessage: String, throwable: Throwable?): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.BAD_REQUEST, errorMessage, throwable)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.FORBIDDEN, errorMessage, null)

    fun <T> illegalState(errorMessage: String, throwable: Throwable): ResponseEntity<Ressurs<T>> =
        errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, throwable)

    fun <T> ok(data: T): ResponseEntity<Ressurs<T>> = ResponseEntity.ok(Ressurs.success(data))
    fun <T> created(): ResponseEntity<Ressurs<T>> = ResponseEntity.status(HttpStatus.CREATED).build()

    fun <T> noContent(): ResponseEntity<Ressurs<T>> = ResponseEntity.noContent().build()

    private fun <T> errorResponse(
        httpStatus: HttpStatus,
        errorMessage: String,
        throwable: Throwable?,
    ): ResponseEntity<Ressurs<T>> {
        val className = if (throwable != null) "[${throwable::class.java.name}] " else ""

        secureLogger.error("$className En feil har oppstått: $errorMessage", throwable)
        LOG.error("$className En feil har oppstått: $errorMessage")
        return ResponseEntity.status(httpStatus).body(Ressurs.failure(errorMessage))
    }
}
