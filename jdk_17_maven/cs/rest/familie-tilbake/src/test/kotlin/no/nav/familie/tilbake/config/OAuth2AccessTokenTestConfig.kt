package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-oauth")
class OAuth2AccessTokenTestConfig {

    @Bean
    @Primary
    fun oAuth2AccessTokenServiceMock(): OAuth2AccessTokenService {
        val tokenMockService: OAuth2AccessTokenService = mockk()
        every { tokenMockService.getAccessToken(any()) }
            .returns(
                OAuth2AccessTokenResponse(
                    "Mock-token-response",
                    60,
                    60,
                    null,
                ),
            )
        return tokenMockService
    }
}
