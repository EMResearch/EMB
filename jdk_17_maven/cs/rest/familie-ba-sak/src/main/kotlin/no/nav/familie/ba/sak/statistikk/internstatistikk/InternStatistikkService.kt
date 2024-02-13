package no.nav.familie.ba.sak.statistikk.internstatistikk

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.springframework.stereotype.Service

@Service
class InternStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
) {
    fun finnAntallFagsakerTotalt() = fagsakRepository.finnAntallFagsakerTotalt()
    fun finnAntallFagsakerLøpende() = fagsakRepository.finnAntallFagsakerLøpende()
    fun finnAntallBehandlingerIkkeErAvsluttet() = behandlingRepository.finnAntallBehandlingerIkkeAvsluttet()
    fun finnAntallBehandlingerPerÅrsak() =
        behandlingRepository.finnAntallBehandlingerPerÅrsak().associate { it.first to it.second }
}
