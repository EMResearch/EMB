package no.nav.familie.tilbake.kravgrunnlag.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Kravstatuskode(@JsonValue val kode: String, val navn: String) {

    ANNULERT("ANNU", "Kravgrunnlag annullert"),
    ANNULLERT_OMG("ANOM", "Kravgrunnlag annullert ved omg"),
    AVSLUTTET("AVSL", "Avsluttet kravgrunnlag"),
    BEHANDLET("BEHA", "Kravgrunnlag ferdigbehandlet"),
    ENDRET("ENDR", "Endret kravgrunnlag"),
    FEIL("FEIL", "Feil på kravgrunnlag"),
    MANUELL("MANU", "Manuell behandling"),
    NYTT("NY", "Nytt kravgrunnlag"),
    SPERRET("SPER", "Kravgrunnlag sperret"),
    ;

    companion object {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fraKode(kode: String): Kravstatuskode {
            for (kravstatuskode in values()) {
                if (kode == kravstatuskode.kode) {
                    return kravstatuskode
                }
            }
            throw IllegalArgumentException("Kravstatuskode finnes ikke for kode $kode")
        }
    }
}
