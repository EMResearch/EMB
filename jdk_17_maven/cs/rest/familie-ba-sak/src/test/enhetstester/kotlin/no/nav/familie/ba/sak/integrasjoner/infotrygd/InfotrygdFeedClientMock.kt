package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class InfotrygdFeedClientMock {

    @Bean
    @Profile("mock-infotrygd-feed")
    @Primary
    fun mockInfotrygdFeed(): InfotrygdFeedClient {
        return mockk(relaxed = true)
    }
}
