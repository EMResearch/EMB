package no.nav.familie.ba.sak.kjerne.logg

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling

data class BehandlingLoggRequest(val behandling: Behandling, val barnasIdenter: List<String> = emptyList())
