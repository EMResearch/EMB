package no.nav.familie.ba.sak.cucumber.domeneparser

object BrevPeriodeParser {

    enum class DomenebegrepBrevBegrunnelse(override val nøkkel: String) : Domenenøkkel {
        BEGRUNNELSE("Begrunnelse"),
        GJELDER_SØKER("Gjelder søker"),
        BARNAS_FØDSELSDATOER("Barnas fødselsdatoer"),
        ANTALL_BARN("Antall barn"),
        MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR("Måned og år begrunnelsen gjelder for"),
        MÅLFORM("Målform"),
        BELØP("Beløp"),
        SØKNADSTIDSPUNKT("Søknadstidspunkt"),
        AVTALETIDSPUNKT_DELT_BOSTED("Avtaletidspunkt delt bosted"),
        SØKERS_RETT_TIL_UTVIDET("Søkers rett til utvidet"),
        TYPE("Type"),
    }

    enum class DomenebegrepBrevPeriode(override val nøkkel: String) : Domenenøkkel {
        BARNAS_FØDSELSDAGER("Barnas fødselsdager"),
        ANTALL_BARN("Antall barn med utbetaling"),
        TYPE("Brevperiodetype"),
        BELØP("Beløp"),
        DU_ELLER_INSTITUSJONEN("Du eller institusjonen"),
    }
}
