package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"

        // Release
        const val EØS_INFORMASJON_OM_ÅRLIG_KONTROLL = "familie-ba-sak.eos-informasjon-om-aarlig-kontroll"
        const val ER_MANUEL_POSTERING_TOGGLE_PÅ = "familie-ba-sak.manuell-postering"
        const val FEILUTBETALT_VALUTA_PR_MND = "familie-ba-sak.feilutbetalt-valuta-pr-mnd"
        const val EØS_PRAKSISENDRING_SEPTEMBER2023 =
            "familie-ba-sak.behandling.eos-annen-forelder-omfattet-av-norsk-lovgivning"

        // unleash toggles for satsendring, kan slettes etter at satsendring er skrudd på for alle satstyper
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"
        const val SATSENDRING_SNIKE_I_KØEN = "familie-ba-sak.satsendring-snike-i-koen"

        // Ny utbetalingsgenerator
        const val KONTROLLER_NY_UTBETALINGSGENERATOR = "familie.ba.sak.kontroller-ny-utbetalingsgenerator"
        const val BRUK_NY_UTBETALINGSGENERATOR = "familie.ba.sak.bruk-ny-utbetalingsgenerator"

        // Unleash Next toggles
        const val ENDRET_EØS_REGELVERKFILTER_FOR_BARN = "familie-ba-sak.endret-eos-regelverkfilter-for-barn"
        const val NY_GENERERING_AV_BREVOBJEKTER = "familie-ba-sak.ny-generering-av-brevobjekter"
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
