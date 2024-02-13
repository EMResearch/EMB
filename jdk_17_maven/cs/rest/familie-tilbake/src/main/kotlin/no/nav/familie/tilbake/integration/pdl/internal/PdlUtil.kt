package no.nav.familie.tilbake.integration.pdl.internal

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
val logger: Logger = LoggerFactory.getLogger("PdlUtil")

inline fun <reified T : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<T>): Map<String, T> {
    if (pdlResponse.data == null) {
        secureLogger.error("Data fra pdl er null ved bolkoppslag av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw Feil("Data er null fra PDL -  ${T::class}. Se secure logg for detaljer.")
    }

    val feil = pdlResponse.data.personBolk.filter { it.code != "ok" }.associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        secureLogger.error("Feil ved henting av ${T::class} fra PDL: $feil")
        throw Feil("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
}
