package no.nav.familie.ba.sak.sikkerhet

import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RollestyringMotDatabase {

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @PrePersist
    @PreUpdate
    @PreRemove
    fun kontrollerSkrivetilgang(objekt: Any) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (!harSkrivetilgang(høyesteRolletilgang)) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke skrivetilgang til databasen.",
                frontendFeilmelding = "Du har ikke tilgang til å gjøre denne handlingen.",
            )
        }
    }

    private fun harSkrivetilgang(høyesteRolletilgang: BehandlerRolle) =
        høyesteRolletilgang.nivå >= BehandlerRolle.SAKSBEHANDLER.nivå
}
