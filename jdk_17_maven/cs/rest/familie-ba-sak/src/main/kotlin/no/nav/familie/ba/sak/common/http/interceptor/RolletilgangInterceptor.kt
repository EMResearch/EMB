package no.nav.familie.ba.sak.common.http.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
@Import(RolleConfig::class)
class RolletilgangInterceptor(private val rolleConfig: RolleConfig) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
        SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.VEILEDER)
            .takeIf { it != BehandlerRolle.UKJENT }
            ?.let { super.preHandle(request, response, handler) }
            ?: run {
                logger.info("Bruker ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bruker har ikke tilgang")
                false
            }

    companion object {

        private val logger = LoggerFactory.getLogger(RolletilgangInterceptor::class.java)
    }
}
