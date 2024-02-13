package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
) {

    @Transactional
    fun initierOgSettBehandlingTilVilkårsvurdering(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean = true,
    ) {
        vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
            forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                behandling,
            ),
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak = vedtak)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING,
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)

        vedtakRepository.saveAndFlush(vedtak)
    }

    @Transactional
    fun tilbakestillBehandlingTilVilkårsvurdering(behandling: Behandling) {
        if (behandling.status != BehandlingStatus.UTREDES && behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            throw Feil("Prøver å tilbakestille $behandling, men den er avsluttet eller låst for endringer")
        }

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId = behandling.id)
        vedtak?.let { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak = vedtak) }
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING,
        )
    }

    @Transactional
    fun tilbakestillDataTilVilkårsvurderingssteg(behandling: Behandling) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandlingId = behandling.id,
            ),
        )
    }

    /**
     * Når et vilkår vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun resettStegVedEndringPåVilkår(behandlingId: Long): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandling.id,
            ),
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling.apply { resultat = Behandlingsresultat.IKKE_VURDERT })
        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING,
        )
    }
}
