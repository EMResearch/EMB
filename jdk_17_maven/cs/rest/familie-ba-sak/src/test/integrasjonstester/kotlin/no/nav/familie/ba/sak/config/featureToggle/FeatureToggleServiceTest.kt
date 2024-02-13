package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("integration")
class FeatureToggleServiceTest(
    @Autowired
    private val featureToggleService: FeatureToggleService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `skal svare true ved dummy impl`() {
        Assertions.assertEquals(true, featureToggleService.isEnabled("sull-bala-tull"))
    }
}
