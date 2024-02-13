package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class OAuth2AccessTokenTestConfig {

    @Bean
    @Primary
    @Profile("mock-oauth")
    fun oAuth2AccessTokenServiceMock(): OAuth2AccessTokenService {
        val tokenMockService: OAuth2AccessTokenService = mockk()
        every { tokenMockService.getAccessToken(any()) } returns OAuth2AccessTokenResponse(
            "Mock-token-response",
            60,
            60,
            null,
        )
        return tokenMockService
    }
}
