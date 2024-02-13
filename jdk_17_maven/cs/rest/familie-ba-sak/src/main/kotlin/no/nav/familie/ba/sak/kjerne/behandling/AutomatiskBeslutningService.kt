package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import org.springframework.stereotype.Service

@Service
class AutomatiskBeslutningService(private val simuleringService: SimuleringService) {

    fun behandlingSkalAutomatiskBesluttes(behandling: Behandling): Boolean {
        val harMigreringsbehandlingAvvikInnenforbeløpsgrenser by lazy {
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)
        }

        val harMigreringsbehandlingManuellePosteringer by lazy {
            simuleringService.harMigreringsbehandlingManuellePosteringer(behandling)
        }
        return (behandling.erHelmanuellMigrering() && harMigreringsbehandlingAvvikInnenforbeløpsgrenser && !harMigreringsbehandlingManuellePosteringer) || behandling.erManuellMigreringForEndreMigreringsdato()
    }
}
