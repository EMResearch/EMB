package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingTilBehandlingsresultatService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    /**
     * Når en andel vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun tilbakestillBehandlingTilBehandlingsresultat(behandlingId: Long): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        if (behandling.erTilbakestiltTilBehandlingsresultat()) {
            return behandling
        }

        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandlingId,
            ),
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.BEHANDLINGSRESULTAT,
        )
    }
}

private fun Behandling.erTilbakestiltTilBehandlingsresultat(): Boolean {
    val gjeldendeSteg = this.behandlingStegTilstand.last()
    return gjeldendeSteg.behandlingSteg == StegType.BEHANDLINGSRESULTAT &&
        gjeldendeSteg.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT
}
