package no.nav.familie.ba.sak.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@Profile("!integrasjonstest")
class PensjonConfig(
    private val oidcUtil: OIDCUtil,
    private val rolleConfig: RolleConfig,
) {

    @Bean
    fun pensjonFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            val clientNavn: String? = try {
                oidcUtil.getClaim("azp_name")
            } catch (throwable: Throwable) {
                null
            }
            val erKallerPensjon = clientNavn?.contains("omsorgsopptjening") ?: false
            val harForvalterRolle = SikkerhetContext.harInnloggetBrukerForvalterRolle(rolleConfig)
            val erPensjonRequest = request.requestURI.startsWith("/api/ekstern/pensjon")

            when {
                erKallerPensjon && !erPensjonRequest -> {
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Pensjon applikasjon kan ikke kalle andre tjenester",
                    )
                }
                erPensjonRequest && (!harForvalterRolle && !erKallerPensjon) -> {
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Pensjon tjeneste kan kun kalles av pensjon eller innlogget bruker med FORVALTER rolle",
                    )
                }
                erPensjonRequest && (harForvalterRolle || erKallerPensjon) -> filterChain.doFilter(request, response)
                !erPensjonRequest && !erKallerPensjon -> filterChain.doFilter(request, response)
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/v3") // i bruk av swagger
    }
}
