package no.nav.familie.tilbake.datavarehus.saksstatistikk

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.SærligeGrunner
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.UtvidetVilkårsresultat
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.VedtakPeriode
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.UUID

@Service
class VedtaksoppsummeringService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val foreldelseRepository: VurdertForeldelseRepository,
    private val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
    private val beregningService: TilbakekrevingsberegningService,
) {

    fun hentVedtaksoppsummering(behandlingId: UUID): Vedtaksoppsummering {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val eksternBehandling = behandling.aktivFagsystemsbehandling.eksternId
        val behandlingsvedtak = behandling.sisteResultat?.behandlingsvedtak
            ?: error("Behandling med id=$behandlingId mangler vedtak.Kan ikke sende data til DVH")
        val behandlingsårsak = behandling.årsaker.firstOrNull()
        val forrigeBehandling = behandlingsårsak?.originalBehandlingId?.let { behandlingRepository.findByIdOrNull(it) }
        val ansvarligBeslutter =
            behandling.ansvarligBeslutter
                ?: error("Behandling med Id=$behandlingId mangler ansvarlig beslutter. Kan ikke sende data til DVH")
        return Vedtaksoppsummering(
            behandlingUuid = behandling.eksternBrukId,
            saksnummer = fagsak.eksternFagsakId,
            ytelsestype = fagsak.ytelsestype,
            ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
            ansvarligBeslutter = ansvarligBeslutter,
            behandlingstype = behandling.type,
            behandlingOpprettetTidspunkt = behandling.opprettetTidspunkt.atOffset(ZoneOffset.UTC),
            vedtakFattetTidspunkt = behandlingsvedtak.sporbar.opprettetTid.atOffset(ZoneOffset.UTC),
            referertFagsaksbehandling = eksternBehandling,
            behandlendeEnhet = behandling.behandlendeEnhet,
            erBehandlingManueltOpprettet = behandling.manueltOpprettet,
            forrigeBehandling = forrigeBehandling?.let(Behandling::eksternBrukId),
            perioder = hentVedtakPerioder(behandlingId),
        )
    }

    private fun hentVedtakPerioder(behandlingId: UUID): List<VedtakPeriode> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val beregningsresultat = beregningService.beregn(behandlingId)
        val vilkårsperioder = vilkårsvurdering?.let {
            hentVilkårPerioder(behandlingId, beregningsresultat, it)
        } ?: emptyList()
        val foreldelsesperioder = vurdertForeldelse?.let {
            hentForeldelsePerioder(behandlingId, beregningsresultat, it)
        } ?: emptyList()
        return vilkårsperioder + foreldelsesperioder
    }

    private fun hentVilkårPerioder(
        behandlingId: UUID,
        beregningsresultat: Beregningsresultat,
        vilkårsvurdering: Vilkårsvurdering,
    ): List<VedtakPeriode> {
        return vilkårsvurdering.perioder.map { periode ->
            val beregningsresultatsperiode: Beregningsresultatsperiode =
                beregningsresultat.beregningsresultatsperioder.first { it.periode == periode.periode }
            val faktaFeilutbetaling =
                faktaFeilutbetalingRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
            val faktaperiode = faktaFeilutbetaling.perioder.first { it.periode.overlapper(periode.periode) }

            VedtakPeriode(
                fom = periode.periode.fomDato,
                tom = periode.periode.tomDato,
                hendelsestype = faktaperiode.hendelsestype.name,
                hendelsesundertype = faktaperiode.hendelsesundertype.name,
                harBruktSjetteLedd = periode.aktsomhet?.tilbakekrevSmåbeløp == false,
                aktsomhet = periode.aktsomhet?.aktsomhet,
                vilkårsresultat = UtvidetVilkårsresultat.valueOf(periode.vilkårsvurderingsresultat.name),
                særligeGrunner = hentSærligGrunner(periode),
                feilutbetaltBeløp = beregningsresultatsperiode.feilutbetaltBeløp,
                bruttoTilbakekrevingsbeløp = beregningsresultatsperiode.tilbakekrevingsbeløp,
                rentebeløp = beregningsresultatsperiode.rentebeløp,
            )
        }
    }

    private fun hentForeldelsePerioder(
        behandlingId: UUID,
        beregningsresultat: Beregningsresultat,
        vurdertForeldelse: VurdertForeldelse,
    ): List<VedtakPeriode> {
        return vurdertForeldelse.foreldelsesperioder.mapNotNull { periode ->
            if (periode.erForeldet()) {
                val resultatPeriode: Beregningsresultatsperiode =
                    beregningsresultat.beregningsresultatsperioder.first { it.periode == periode.periode }
                val faktaFeilutbetalingEntitet =
                    faktaFeilutbetalingRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
                val faktaPeriode = faktaFeilutbetalingEntitet.perioder.first { it.periode.overlapper(periode.periode) }

                VedtakPeriode(
                    fom = periode.periode.fomDato,
                    tom = periode.periode.tomDato,
                    hendelsestype = faktaPeriode.hendelsestype.name,
                    hendelsesundertype = faktaPeriode.hendelsesundertype.name,
                    vilkårsresultat = UtvidetVilkårsresultat.FORELDET,
                    feilutbetaltBeløp = resultatPeriode.feilutbetaltBeløp,
                    bruttoTilbakekrevingsbeløp = resultatPeriode.tilbakekrevingsbeløp,
                    rentebeløp = resultatPeriode.rentebeløp,
                )
            } else {
                null
            }
        }
    }

    private fun hentSærligGrunner(periodeEntitet: Vilkårsvurderingsperiode): SærligeGrunner? {
        if (periodeEntitet.aktsomhet?.vilkårsvurderingSærligeGrunner?.isEmpty() == false) {
            val særligeGrunner =
                periodeEntitet.aktsomhet.vilkårsvurderingSærligeGrunner.map(VilkårsvurderingSærligGrunn::særligGrunn)
            return SærligeGrunner(periodeEntitet.aktsomhet.særligeGrunnerTilReduksjon, særligeGrunner)
        }
        return null
    }
}
