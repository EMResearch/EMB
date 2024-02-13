package no.nav.familie.ba.sak.config

import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment

@TestConfiguration
class UnleashServiceMockConfig(
    private val unleashService: UnleashService,
    private val environment: Environment,
) {

    @Bean
    @Primary
    fun mockUnleashService(): UnleashService {
        if (environment.erAktiv(Profil.Integrasjonstest)) {
            val mockUnleashService = mockk<UnleashService>(relaxed = true)

            ClientMocks.clearUnleashServiceMocks(mockUnleashService)

            return mockUnleashService
        }
        return unleashService
    }
}
