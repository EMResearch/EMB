package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.LocalDateService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class LocalDateServiceTestConfig {

    @Bean
    @Profile("mock-localdate-service")
    @Primary
    fun mockLocalDateService(): LocalDateService {
        val mockLocalDateService = mockk<LocalDateService>()

        clearLocalDateServiceMocks(mockLocalDateService)

        return mockLocalDateService
    }

    companion object {
        fun clearLocalDateServiceMocks(mockLocalDateService: LocalDateService) {
            clearMocks(mockLocalDateService)

            every { mockLocalDateService.now() } returns LocalDate.now()
        }
    }
}
