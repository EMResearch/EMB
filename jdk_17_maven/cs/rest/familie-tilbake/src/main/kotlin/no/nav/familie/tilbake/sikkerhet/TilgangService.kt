package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.RolleConfig
import org.springframework.stereotype.Service

@Service
class TilgangService(private val rolleConfig: RolleConfig) {

    fun tilgangTilÅOppretteRevurdering(fagsystem: Fagsystem): Boolean {
        return finnBehandlerrolle(fagsystem) !in listOf(Behandlerrolle.VEILEDER, Behandlerrolle.FORVALTER)
    }

    fun finnBehandlerrolle(fagsystem: Fagsystem): Behandlerrolle? {
        val inloggetBrukerstilgang = ContextService
            .hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, "henter behandling")

        val tilganger = inloggetBrukerstilgang.tilganger
        var behandlerrolle: Behandlerrolle? = Behandlerrolle.VEILEDER
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) {
            behandlerrolle = Behandlerrolle.SYSTEM
        }
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            behandlerrolle = Behandlerrolle.FORVALTER
        }
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.fraFagsystem(fagsystem))) {
            behandlerrolle = tilganger[Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)]
        }
        return behandlerrolle
    }
}
