package no.nav.familie.tilbake.config

import io.getunleash.strategy.Strategy
import no.nav.familie.unleash.UnleashService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Configuration
class FeatureToggleConfig(@Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String) {

    @Bean
    fun strategies(): List<Strategy> {
        return listOf(ByClusterStrategy(clusterName))
    }

    companion object {

        const val KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK =
            "familie-tilbake.beh.kanopprettes.eksternid.avsluttet.tilbakekreving"

        const val OVERSTYR_DELVILS_TILBAKEKREVING_TIL_FULL_TILBAKEKREVING = "familie-tilbake.overstyr-delvis-hvis-full"

        const val BRUK_6_DESIMALER_I_SKATTEBEREGNING = "familie-tilbake.bruk-seks-desimaler-skatt"

        const val IKKE_VALIDER_SÆRLIG_GRUNNET_ANNET_FRITEKST =
            "familie-tilbake.ikke-valider-saerlig-grunnet-annet-fritekst"
    }
}

@Service
@Profile("!integrasjonstest")
class FeatureToggleService(val unleashService: UnleashService) {

    fun isEnabled(toggleId: String): Boolean {
        return unleashService.isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return unleashService.isEnabled(toggleId, defaultValue)
    }
}

class ByClusterStrategy(private val clusterName: String) : Strategy {

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        if (parameters.isEmpty()) return false
        return parameters["cluster"]?.contains(clusterName) ?: false
    }

    override fun getName(): String = "byCluster"
}
