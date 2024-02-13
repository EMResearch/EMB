package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.util.tilleggOrdinærSatsNesteMånedTilTester
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FødselshendelseFørstegangsbehandlingTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn født november 2021 og behandles desember 2021 uten utbetalinger`() {
        // Behandler desember 2021 for å få med automatisk begrunnelse av satsendring januar 2022
        every { mockLocalDateService.now() } returns LocalDate.of(2021, 12, 12) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2021, 11, 18).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )
        val behandling = behandleFødselshendelse(
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

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(Behandlingsresultat.INNVILGET, aktivBehandling.resultat)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = aktivBehandling.behandlingId)
        val vedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak = vedtak)

        val desember2021Vedtaksperiode = vedtaksperioder.find { it.fom == LocalDate.of(2021, 12, 1) }
        val januar2022Vedtaksperiode = vedtaksperioder.find { it.fom == LocalDate.of(2022, 1, 1) }

        assertEquals(
            0,
            vedtaksperioder
                .filter { it != desember2021Vedtaksperiode && it != januar2022Vedtaksperiode }
                .flatMap { it.begrunnelser }
                .size,
        )

        assertEquals(
            1654,
            desember2021Vedtaksperiode?.utbetalingsperiodeDetaljer?.totaltUtbetalt(),
        )
        assertEquals(
            Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE,
            desember2021Vedtaksperiode?.begrunnelser?.first()?.standardbegrunnelse,
        )

        assertEquals(
            1676,
            januar2022Vedtaksperiode?.utbetalingsperiodeDetaljer?.totaltUtbetalt(),
        )
        assertEquals(
            Standardbegrunnelse.INNVILGET_SATSENDRING,
            januar2022Vedtaksperiode?.begrunnelser?.first()?.standardbegrunnelse,
        )
    }

    @Test
    fun `Skal innvilge fødselshendelse på mor med 2 barn uten utbetalinger`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen 2",
                    ),
                ),
            ),
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = scenario.barna.map { it.ident!! },
            ),
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
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(Behandlingsresultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() >= tilleggOrdinærSatsNesteMånedTilTester().gyldigFom.toYearMonth() &&
                it.periodeFom.toYearMonth() <= tilleggOrdinærSatsNesteMånedTilTester().gyldigTom.toYearMonth()
        }!!

        assertUtbetalingsperiode(
            gjeldendeUtbetalingsperiode,
            2,
            tilleggOrdinærSatsNesteMånedTilTester().beløp * 2,
        )
    }
}
