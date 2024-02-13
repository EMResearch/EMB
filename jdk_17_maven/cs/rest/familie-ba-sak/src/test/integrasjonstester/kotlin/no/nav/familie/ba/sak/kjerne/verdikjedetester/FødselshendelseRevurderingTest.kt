package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.util.tilleggOrdinærSatsNesteMånedTilTester
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate.now

class FødselshendelseRevurderingTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        val revurderingsbarnSinFødselsdato = now().minusMonths(3)
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(12).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                    RestScenarioPerson(
                        fødselsdato = revurderingsbarnSinFødselsdato.toString(),
                        fornavn = "Barn2",
                        etternavn = "Barnesen2",
                    ),
                ),
            ),
        )

        behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.minByOrNull { it.fødselsdato }!!.ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,

        )

        val søkerIdent = scenario.søker.ident
        val vurdertBarn = scenario.barna.maxByOrNull { it.fødselsdato }!!.ident!!
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = søkerIdent,
                barnasIdenter = listOf(vurdertBarn),
            ),
            fagsakStatusEtterVurdering = FagsakStatus.LØPENDE,
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)

        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
            aktivBehandlingId = behandling.id,
        )

        val aktivBehandling =
            restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger
                .single {
                    it.behandlingId == behandlingHentOgPersisterService.finnAktivForFagsak(
                        restFagsakEtterBehandlingAvsluttet.data!!.id,
                    )?.id
                }

        val vurderteVilkårIDenneBehandlingen = aktivBehandling.personResultater.flatMap { it.vilkårResultater }
            .filter { it.behandlingId == aktivBehandling.behandlingId }
        assertEquals(Behandlingsresultat.INNVILGET, aktivBehandling.resultat)
        assertEquals(5, vurderteVilkårIDenneBehandlingen.size)
        vurderteVilkårIDenneBehandlingen.forEach { assertEquals(revurderingsbarnSinFødselsdato, it.periodeFom) }

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val nesteMånedUtbetalingsperiode = utbetalingsperioder.sortedBy { it.periodeFom }.first {
            it.periodeFom.toYearMonth() <= now().nesteMåned() && it.periodeTom.toYearMonth() >= now().nesteMåned()
        }

        assertUtbetalingsperiode(
            nesteMånedUtbetalingsperiode,
            2,
            tilleggOrdinærSatsNesteMånedTilTester().beløp * 2,
        )
    }
}
