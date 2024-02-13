package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class FeatureToggleInitializer(
    private val featureToggleProperties: FeatureToggleProperties,
    private val environment: Environment,
) {

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (featureToggleProperties.enabled || environment.erAktiv(Profil.DevPostgresPreprod)) {
            UnleashFeatureToggleService(featureToggleProperties.unleash)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'",
            )
            DummyFeatureToggleService(featureToggleProperties.unleash)
        }

    companion object {

        private val logger = LoggerFactory.getLogger(FeatureToggleProperties::class.java)
    }
}
