package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.valider18ÅrsVilkårEksistererFraFødselsdato
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIkkeBlandetRegelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIngenVilkårSattEtterSøkersDød
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val tilpassKompetanserTilRegelverkService: TilpassKompetanserTilRegelverkService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
            validerIngenVilkårSattEtterSøkersDød(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = vilkårsvurdering,
            )
        }

        vilkårService.hentVilkårsvurdering(behandling.id)?.apply {
            validerIkkeBlandetRegelverk(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = this,
            )

            valider18ÅrsVilkårEksistererFraFødselsdato(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = this,
                behandling = behandling,
            )
        }
    }

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = true,
                forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                    behandling,
                ),
            )
        }

        tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling)
        beregningService.genererTilkjentYtelseFraVilkårsvurdering(behandling, personopplysningGrunnlag)

        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id))

        behandlingstemaService.oppdaterBehandlingstema(
            behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id),
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }
}
