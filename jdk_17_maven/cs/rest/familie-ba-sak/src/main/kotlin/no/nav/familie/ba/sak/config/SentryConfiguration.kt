package no.nav.familie.ba.sak.config

import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.User
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils

@Configuration
class SentryConfiguration(
    @Value("\${sentry.environment}") val environment: String,
    @Value("\${sentry.dsn}") val dsn: String,
    @Value("\${sentry.logging.enabled}") val enabled: Boolean,
) {
    init {
        Sentry.init { options ->
            options.dsn = if (enabled) dsn else "" // Tom streng betryr at Sentry er disabled
            options.environment = environment
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                Sentry.configureScope { scope ->
                    scope.user = User().apply {
                        id = SikkerhetContext.hentSaksbehandler()
                        email = SikkerhetContext.hentSaksbehandlerEpost()
                        username = SikkerhetContext.hentSaksbehandler()
                    }
                }

                val mostSpecificThrowable =
                    if (event.throwable != null) NestedExceptionUtils.getMostSpecificCause(event.throwable!!) else event.throwable
                val metodeSomFeiler = finnMetodeSomFeiler(mostSpecificThrowable)
                val prosess = MDC.get("prosess")

                event.setTag("metodeSomFeier", metodeSomFeiler)
                event.setTag("bruker", SikkerhetContext.hentSaksbehandlerEpost())
                event.setTag("kibanalenke", hentKibanalenke(MDC.get("callId")))
                event.setTag("prosess", prosess)

                event.fingerprints = listOf(
                    "{{ default }}",
                    prosess,
                    event.transaction,
                    mostSpecificThrowable?.message,
                )

                if (metodeSomFeiler != UKJENT_METODE_SOM_FEILER) {
                    event.fingerprints = (event.fingerprints ?: emptyList()) + listOf(
                        metodeSomFeiler,
                    )
                }

                event
            }
        }
    }

    private fun hentKibanalenke(callId: String) =
        "https://logs.adeo.no/app/discover#/?_g=(time:(from:now-1M,to:now))&_a=(filters:!((query:(match_phrase:(x_callId:'$callId')))))"

    fun finnMetodeSomFeiler(e: Throwable?): String {
        val firstElement = e?.stackTrace?.firstOrNull {
            it.className.startsWith("no.nav.familie.ba.sak") &&
                !it.className.contains("$")
        }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e?.cause?.let { finnMetodeSomFeiler(it) } ?: UKJENT_METODE_SOM_FEILER
    }

    companion object {
        val logger = LoggerFactory.getLogger(SentryConfiguration::class.java)
        const val UKJENT_METODE_SOM_FEILER = "(Ukjent metode som feiler)"
    }
}
