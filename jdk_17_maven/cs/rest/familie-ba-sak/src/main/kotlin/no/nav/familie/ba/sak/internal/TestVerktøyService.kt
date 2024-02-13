package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.internal.vedtak.begrunnelser.lagGyldigeBegrunnelserTest
import no.nav.familie.ba.sak.internal.vedtak.vedtaksperioder.lagVedtaksperioderTest
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestVerktøyService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingRepository: EndretUtbetalingAndelRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakService: VedtakService,
    private val kompetanseRepository: KompetanseRepository,
) {

    @Transactional
    fun oppdaterVilkårUtenFomTilFødselsdato(behandlingId: Long) {
        val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId)

        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)

        vilkårsvurdering?.personResultater?.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (vilkårResultat.resultat == Resultat.IKKE_VURDERT) {
                    vilkårResultat.periodeFom =
                        persongrunnlag?.personer?.find { it.aktør == personResultat.aktør }?.fødselsdato
                    vilkårResultat.resultat = Resultat.OPPFYLT
                    vilkårResultat.begrunnelse = "Opprettet automatisk fra \"Fyll ut vilkårsvurdering\"-knappen"
                }
            }
        }
    }

    fun hentBegrunnelsetest(behandlingId: Long): String {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        val persongrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling =
            forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }

        val personResultater = vilkårService.hentVilkårsvurderingThrows(behandlingId).personResultater
        val personResultaterForrigeBehandling =
            forrigeBehandling?.let { vilkårService.hentVilkårsvurderingThrows(it.id).personResultater }

        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }

        val endredeUtbetalinger = endretUtbetalingRepository.findByBehandlingId(behandlingId)
        val endredeUtbetalingerForrigeBehandling =
            forrigeBehandling?.let { endretUtbetalingRepository.findByBehandlingId(it.id) }

        val kompetanse = kompetanseRepository.finnFraBehandlingId(behandlingId)
        val kompetanseForrigeBehandling =
            forrigeBehandling?.let { kompetanseRepository.finnFraBehandlingId(it.id) }

        val vedtaksperioder = vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(
            vedtakService.hentAktivForBehandlingThrows(behandlingId).id,
        )

        return lagGyldigeBegrunnelserTest(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vedtaksperioder = vedtaksperioder,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
        )
    }

    fun hentVedtaksperioderTest(behandlingId: Long): String {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        val persongrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling =
            forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }
        val personResultater = vilkårService.hentVilkårsvurderingThrows(behandlingId).personResultater
        val personResultaterForrigeBehandling =
            forrigeBehandling?.let { vilkårService.hentVilkårsvurderingThrows(it.id).personResultater }
        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
        val endredeUtbetalinger = endretUtbetalingRepository.findByBehandlingId(behandlingId)
        val endredeUtbetalingerForrigeBehandling =
            forrigeBehandling?.let { endretUtbetalingRepository.findByBehandlingId(it.id) }
        val kompetanse = kompetanseRepository.finnFraBehandlingId(behandlingId)
        val kompetanseForrigeBehandling =
            forrigeBehandling?.let { kompetanseRepository.finnFraBehandlingId(it.id) }
        val vedtaksperioder = vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(
            vedtakService.hentAktivForBehandlingThrows(behandlingId).id,
        )

        return lagVedtaksperioderTest(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vedtaksperioder = vedtaksperioder,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
        )
    }
}
