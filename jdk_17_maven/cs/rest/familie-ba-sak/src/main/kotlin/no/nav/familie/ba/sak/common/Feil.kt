package no.nav.familie.ba.sak.common

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Feil(
    message: String,
    open val frontendFeilmelding: String? = null,
    open val httpStatus: HttpStatus = HttpStatus.OK,
    open val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,

) : RuntimeException(message)

open class FunksjonellFeil(
    open val melding: String,
    open val frontendFeilmelding: String? = melding,
    open val httpStatus: HttpStatus = HttpStatus.OK,
    open val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
) : RuntimeException(melding)

class UtbetalingsikkerhetFeil(
    melding: String,
    override val frontendFeilmelding: String? = null,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
) : FunksjonellFeil(
    melding,
    frontendFeilmelding,
    httpStatus,
    throwable,
)

class RolleTilgangskontrollFeil(
    melding: String,
    override val frontendFeilmelding: String = melding,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
) : FunksjonellFeil(
    melding,
    frontendFeilmelding,
    httpStatus,
    throwable,
)

class PdlRequestException(message: String) : Feil(message)
class PdlNotFoundException : FunksjonellFeil("Fant ikke person")
class PdlPersonKanIkkeBehandlesIFagsystem(val årsak: String) :
    FunksjonellFeil("Person kan ikke behandles i fagsystem: $årsak")

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder(value = ["melding", "path", "timestamp", "status", "exception", "stackTrace"])
data class EksternTjenesteFeil(
    val path: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    var exception: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    var stackTrace: String? = null,
) {
    lateinit var melding: String
}

open class EksternTjenesteFeilException(
    val eksternTjenesteFeil: EksternTjenesteFeil,
    val melding: String,
    val request: Any?,
    val throwable: Throwable? = null,
) : RuntimeException(melding, throwable) {

    init {
        eksternTjenesteFeil.melding = melding
    }

    override fun toString(): String {
        return """EksternTjenesteFeil(
            |   melding='$melding' 
            |   eksternTjeneste=$eksternTjenesteFeil
            |   request=$request
            |   throwable=$throwable)
        """.trimMargin()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun feilHvis(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, lazyMessage: () -> String) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(message = lazyMessage(), frontendFeilmelding = lazyMessage(), httpStatus)
    }
}
