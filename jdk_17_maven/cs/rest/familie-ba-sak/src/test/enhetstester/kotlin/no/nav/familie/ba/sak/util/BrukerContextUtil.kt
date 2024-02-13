package no.nav.familie.ba.sak.util

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

object BrukerContextUtil {

    fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        servletRequest: HttpServletRequest = MockHttpServletRequest(),
    ) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims = mockk<JwtTokenClaims>()
        val requestAttributes = ServletRequestAttributes(servletRequest)
        RequestContextHolder.setRequestAttributes(requestAttributes)
        requestAttributes.setAttribute(
            SpringTokenValidationContextHolder::class.java.name,
            tokenValidationContext,
            RequestAttributes.SCOPE_REQUEST,
        )
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") } returns preferredUsername
        every { jwtTokenClaims.get("NAVident") } returns preferredUsername
        every { jwtTokenClaims.get("name") } returns preferredUsername
        every { jwtTokenClaims.get("groups") } returns groups
        every { jwtTokenClaims.get("oid") } returns UUID.randomUUID().toString()
        every { jwtTokenClaims.get("sub") } returns UUID.randomUUID().toString()
    }

    fun <T> testWithBrukerContext(preferredUsername: String = "A", groups: List<String> = emptyList(), fn: () -> T): T {
        try {
            mockBrukerContext(preferredUsername, groups)
            return fn()
        } finally {
            clearBrukerContext()
        }
    }
}
