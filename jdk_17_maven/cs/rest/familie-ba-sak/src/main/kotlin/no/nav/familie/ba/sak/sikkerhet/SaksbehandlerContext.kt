package no.nav.familie.ba.sak.sikkerhet

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SaksbehandlerContext(
    @Value("\${rolle.kode6}")
    private val kode6GruppeId: String,
) {

    fun hentSaksbehandlerSignaturTilBrev(): String {
        val grupper = SikkerhetContext.hentGrupper()

        return if (grupper.contains(kode6GruppeId)) {
            ""
        } else {
            SikkerhetContext.hentSaksbehandlerNavn()
        }
    }
}
