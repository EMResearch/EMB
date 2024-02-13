package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType

class RestBehandlingStegTilstand(
    val behandlingSteg: StegType,
    val behandlingStegStatus: BehandlingStegStatus,
)

fun BehandlingStegTilstand.tilRestBehandlingStegTilstand() =
    RestBehandlingStegTilstand(
        behandlingSteg = this.behandlingSteg,
        behandlingStegStatus = this.behandlingStegStatus,
    )
