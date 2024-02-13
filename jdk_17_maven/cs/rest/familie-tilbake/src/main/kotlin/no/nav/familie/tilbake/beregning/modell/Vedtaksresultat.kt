package no.nav.familie.tilbake.beregning.modell

enum class Vedtaksresultat(val navn: String) {
    // Kun brukes for å sende data til frontend
    FULL_TILBAKEBETALING("Tilbakebetaling"),
    DELVIS_TILBAKEBETALING("Delvis tilbakebetaling"),
    INGEN_TILBAKEBETALING("Ingen tilbakebetaling"),
}
