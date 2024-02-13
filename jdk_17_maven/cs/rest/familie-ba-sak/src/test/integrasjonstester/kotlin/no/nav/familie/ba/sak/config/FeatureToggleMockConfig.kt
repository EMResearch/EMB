package no.nav.familie.ba.sak.config

import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleInitializer
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment

@TestConfiguration
class FeatureToggleMockConfig(
    @Autowired val featureToggleInitializer: FeatureToggleInitializer,
    @Autowired private val environment: Environment,
) {

    @Bean
    @Primary
    fun mockFeatureToggleService(): FeatureToggleService {
        if (environment.erAktiv(Profil.Integrasjonstest)) {
            val mockFeatureToggleService = mockk<FeatureToggleService>(relaxed = true)

            ClientMocks.clearFeatureToggleMocks(mockFeatureToggleService)

            return mockFeatureToggleService
        }
        return featureToggleInitializer.featureToggle()
    }
}
