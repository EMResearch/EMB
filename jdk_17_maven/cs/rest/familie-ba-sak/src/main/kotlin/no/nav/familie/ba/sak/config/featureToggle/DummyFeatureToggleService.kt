package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService

class DummyFeatureToggleService(
    private val unleash: FeatureToggleProperties.Unleash,
) : FeatureToggleService {

    private val overstyrteBrytere = mapOf(
        Pair(FeatureToggleConfig.TEKNISK_VEDLIKEHOLD_HENLEGGELSE, true),
    )

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        if (unleash.cluster == "lokalutvikling") {
            return true
        }

        return overstyrteBrytere.getOrDefault(toggleId, true)
    }
}
