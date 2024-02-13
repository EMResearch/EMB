package no.nav.familie.ba.sak.sikkerhet

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev")
class AuditLoggerMock {

    @Bean
    @Primary
    fun auditLogger(): AuditLogger = mockk(relaxed = true)
}
