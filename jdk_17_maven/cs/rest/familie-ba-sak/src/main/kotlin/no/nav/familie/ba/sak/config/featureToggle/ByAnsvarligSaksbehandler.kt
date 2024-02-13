package no.nav.familie.ba.sak.config.featureToggle

import io.getunleash.strategy.Strategy
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

class ByAnsvarligSaksbehandler : Strategy {

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        if (parameters.isEmpty()) return false

        return parameters["saksbehandler"]?.contains(SikkerhetContext.hentSaksbehandlerEpost()) ?: false
    }

    override fun getName(): String = "byAnsvarligSaksbehandler"
}
