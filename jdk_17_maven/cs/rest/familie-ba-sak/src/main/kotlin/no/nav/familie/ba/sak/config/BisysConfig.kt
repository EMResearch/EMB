package no.nav.familie.ba.sak.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class BisysConfig(
    private val oidcUtil: OIDCUtil,
    @Value("\${BISYS_CLIENT_ID:dummy}")
    private val bisysClientId: String,
) {

    @Bean
    fun bisysFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            val clientId: String? = try {
                oidcUtil.getClaim("azp")
            } catch (throwable: Throwable) {
                null
            }

            if (clientId == null) {
                // Dersom requesten mangler auth token, skal ikke dette filteret gjøre autorisasjonen
                filterChain.doFilter(request, response)
            } else if (bisysClientId == clientId && !request.requestURI.startsWith("/api/bisys")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Kun autorisert for kall mot /api/bisys*")
            } else {
                filterChain.doFilter(request, response)
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/v2") // i bruk av swagger
    }
}
