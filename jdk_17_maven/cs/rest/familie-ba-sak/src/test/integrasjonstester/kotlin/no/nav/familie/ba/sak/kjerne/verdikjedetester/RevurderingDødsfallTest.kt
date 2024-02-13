package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.behandling.kjørStegprosessForBehandling
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingFraRestScenario
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RevurderingDødsfallTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    @Autowired private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Dødsfall bruker skal kjøre gjennom`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1982-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )

        behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,

        )

        val overstyrendeVilkårResultater =
            scenario.barna.associate { it.aktørId!! to emptyList<VilkårResultat>() }.toMutableMap()

        // Ved søkers dødsfall settes tomdatoen for "bosatt i riket"-vilkåret til dagen søker døde.
        overstyrendeVilkårResultater[scenario.søker.aktørId!!] = listOf(
            lagVilkårResultat(
                vilkårType = Vilkår.BOSATT_I_RIKET,
                periodeFom = LocalDate.parse(scenario.søker.fødselsdato),
                periodeTom = LocalDate.now().minusMonths(1),
                personResultat = mockk(relaxed = true),
            ),
            lagVilkårResultat(
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                periodeFom = LocalDate.parse(scenario.søker.fødselsdato),
                periodeTom = LocalDate.now().minusMonths(1),
                personResultat = mockk(relaxed = true),
            ),
        )

        val behandlingDødsfall = kjørStegprosessForBehandling(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident,
            barnasIdenter = listOf(scenario.barna.first().ident!!),
            vedtakService = vedtakService,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.DØDSFALL_BRUKER,
            overstyrendeVilkårsvurdering = lagVilkårsvurderingFraRestScenario(scenario, overstyrendeVilkårResultater),

            behandlingstype = BehandlingType.REVURDERING,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            brevmalService = brevmalService,
        )

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandlingDødsfall.fagsak.id)

        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
            aktivBehandlingId = behandlingDødsfall.id,
        )
    }

    @Test
    fun `Dødsfall bruker skal stoppes dersom ikke bosatt i riket er stoppet før dagens dato`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1982-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )

        behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,

        )

        val overstyrendeVilkårResultater =
            (scenario.barna + scenario.søker).associate { it.aktørId!! to emptyList<VilkårResultat>() }.toMutableMap()

        assertThrows<FunksjonellFeil> {
            kjørStegprosessForBehandling(
                tilSteg = StegType.BEHANDLINGSRESULTAT,
                søkerFnr = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
                vedtakService = vedtakService,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingÅrsak = BehandlingÅrsak.DØDSFALL_BRUKER,
                overstyrendeVilkårsvurdering = lagVilkårsvurderingFraRestScenario(
                    scenario,
                    overstyrendeVilkårResultater,
                ),

                behandlingstype = BehandlingType.REVURDERING,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
                fagsakService = fagsakService,
                persongrunnlagService = persongrunnlagService,
                andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
                brevmalService = brevmalService,
            )
        }
    }
}
