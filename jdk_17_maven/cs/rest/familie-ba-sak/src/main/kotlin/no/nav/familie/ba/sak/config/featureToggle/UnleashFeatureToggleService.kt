package no.nav.familie.ba.sak.config.featureToggle

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.util.UnleashConfig
import no.nav.familie.ba.sak.config.FeatureToggleService

class UnleashFeatureToggleService(unleash: FeatureToggleProperties.Unleash) : FeatureToggleService {

    private val defaultUnleash: DefaultUnleash
    private val unleash: FeatureToggleProperties.Unleash

    init {
        defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI(unleash.uri)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            ByClusterStrategy(unleash.cluster),
            ByAnsvarligSaksbehandler(),
            GradualRolloutRandomStrategy(),
        )
        this.unleash = unleash
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .appName(unleash.applicationName)
                .build()
        }
    }

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return defaultUnleash.isEnabled(toggleId, defaultValue)
    }
}
