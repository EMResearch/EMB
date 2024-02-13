package no.nav.familie.tilbake.oppgave

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

class OppgaveServiceTestConfig {

    @Bean
    @Profile("mock-oppgave")
    @Primary
    fun mockArbeidsfordelingService(): OppgaveService {
        return mockk(relaxed = true)
    }
}
