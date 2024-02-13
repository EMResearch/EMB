package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg

class BehandlingerPerSteg(
    val fagsystem: Fagsystem,
    val behandlingssteg: Behandlingssteg,
    val antall: Int,
)
