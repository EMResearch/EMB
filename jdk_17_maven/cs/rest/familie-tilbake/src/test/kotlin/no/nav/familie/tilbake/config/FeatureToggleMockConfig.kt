package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("integrasjonstest")
class FeatureToggleMockConfig {

    @Bean
    fun featureToggle(): FeatureToggleService {
        val mockFeatureToggleService: FeatureToggleService = mockk()
        val defaultValue = slot<Boolean>()

        every { mockFeatureToggleService.isEnabled(any()) } returns false
        every { mockFeatureToggleService.isEnabled(any(), capture(defaultValue)) } answers {
            defaultValue.captured
        }
        return mockFeatureToggleService
    }
}
