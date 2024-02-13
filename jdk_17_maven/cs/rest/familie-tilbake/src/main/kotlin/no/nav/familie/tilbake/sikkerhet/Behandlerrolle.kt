package no.nav.familie.tilbake.sikkerhet

enum class Behandlerrolle(val nivå: Int) {
    SYSTEM(5),
    BESLUTTER(4),
    SAKSBEHANDLER(3),
    FORVALTER(2),
    VEILEDER(1),
}

class InnloggetBrukertilgang(val tilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>)
