package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class InfotrygdBarnetrygdClientMock {

    @Bean
    @Profile("mock-infotrygd-barnetrygd")
    @Primary
    fun mockInfotrygdBarnetrygd(): InfotrygdBarnetrygdClient {
        val mockInfotrygdBarnetrygdClient = mockk<InfotrygdBarnetrygdClient>(relaxed = true)

        clearInfotrygdBarnetrygdMocks(mockInfotrygdBarnetrygdClient)

        return mockInfotrygdBarnetrygdClient
    }

    companion object {
        fun clearInfotrygdBarnetrygdMocks(mockInfotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {
            clearMocks(mockInfotrygdBarnetrygdClient)

            every { mockInfotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(any(), any()) } returns false
            every { mockInfotrygdBarnetrygdClient.hentSaker(any(), any()) } returns InfotrygdSøkResponse(
                emptyList(),
                emptyList(),
            )
            every { mockInfotrygdBarnetrygdClient.hentStønader(any(), any()) } returns InfotrygdSøkResponse(
                emptyList(),
                emptyList(),
            )
        }
    }
}
