package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment

class EnvServiceTest(
    @Autowired
    private val environment: Environment,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `erDev skal returnere true dersom appen er startet med dev-profil`() {
        val envService = EnvService(environment)

        assertTrue(envService.erDev())
        assertFalse(envService.erProd())
        assertFalse(envService.erPreprod())
    }
}
