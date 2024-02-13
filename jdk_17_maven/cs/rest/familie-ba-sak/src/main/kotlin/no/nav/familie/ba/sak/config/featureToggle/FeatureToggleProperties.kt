package no.nav.familie.ba.sak.config.featureToggle

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
class FeatureToggleProperties(
    val enabled: Boolean,
    val unleash: Unleash,
) {

    data class Unleash(
        val uri: URI,
        val cluster: String,
        val applicationName: String,
    )
}
