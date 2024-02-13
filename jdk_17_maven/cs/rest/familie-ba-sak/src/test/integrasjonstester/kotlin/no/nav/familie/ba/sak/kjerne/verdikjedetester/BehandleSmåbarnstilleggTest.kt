package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.EfSakRestClientMock
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.util.sisteSmåbarnstilleggSatsTilTester
import no.nav.familie.ba.sak.util.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.util.tilleggOrdinærSatsTilTester
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.time.YearMonth

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BehandleSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val autovedtakStegService: AutovedtakStegService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val opprettTaskService: OpprettTaskService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {

    private val barnFødselsdato = LocalDate.now().minusYears(2)
    private val periodeMedFullOvergangsstønadFom = barnFødselsdato.plusYears(1)

    val restScenario = RestScenario(
        søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
            RestScenarioPerson(
                fødselsdato = barnFødselsdato.toString(),
                fornavn = "Barn",
                etternavn = "Barnesen",
                bostedsadresser = emptyList(),
            ),
        ),
    )

    lateinit var scenario: RestScenario

    @BeforeAll
    fun init() {
        scenario = mockServerKlient().lagScenario(restScenario)
    }

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    private fun settOppefSakMockForDeFørste2Testene(søkersIdent: String) {
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = listOf(
                EksternPeriode(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18),
                    datakilde = Datakilde.EF,
                ),
            ),
        )
    }

    @Test
    @Order(1)
    fun `Skal behandle utvidet nasjonal sak med småbarnstillegg`() {
        val søkersIdent = scenario.søker.ident!!
        settOppefSakMockForDeFørste2Testene(søkersIdent)

        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            fagsakId = fagsak.data!!.id,
        )

        val behandling = behandlingHentOgPersisterService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = søkersIdent,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET,
                ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad,
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato,
                            ),
                        ),
                    ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )

        assertEquals(
            tilleggOrdinærSatsTilTester() + sisteUtvidetSatsTilTester() + sisteSmåbarnstilleggSatsTilTester(),
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsResultat.data!!,
            ),
        )

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
            )
        val utvidedeAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
        val småbarnstilleggAndel = andelerTilkjentYtelse.single { it.type == YtelseType.SMÅBARNSTILLEGG }

        assertEquals(
            barnFødselsdato.plusMonths(1).toYearMonth(),
            utvidedeAndeler.minByOrNull { it.stønadFom }?.stønadFom,
        )
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndel.stønadFom,
        )
        assertEquals(
            barnFødselsdato.plusYears(3).toYearMonth(),
            småbarnstilleggAndel.stønadTom,
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsResultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
        )

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
        )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                ),
            ),
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT,
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "azp-test",
                                "name" to "Mock McMockface Beslutter",
                                "NAVident" to "Z0000",
                            ),
                        ),
                    )
                },
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.data!!.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )
    }

    @Test
    @Order(2)
    fun `Skal ikke opprette behandling når det ikke finnes endringer på perioder med full overgangsstønad`() {
        val søkersIdent = scenario.søker.ident!!
        settOppefSakMockForDeFørste2Testene(søkersIdent)

        val søkersAktør = personidentService.hentAktør(søkersIdent)
        autovedtakStegService.kjørBehandlingSmåbarnstillegg(
            mottakersAktør = søkersAktør,
            aktør = søkersAktør,
        )
        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak!!.id)!!

        assertEquals(BehandlingStatus.AVSLUTTET, aktivBehandling.status)
        assertNotEquals(BehandlingÅrsak.SMÅBARNSTILLEGG, aktivBehandling.opprettetÅrsak)
    }

    @Test
    @Order(3)
    fun `Skal stoppe automatisk behandling som må fortsette manuelt pga tilbakekreving`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)

        val søkersAktør = personidentService.hentAktør(scenario.søker.aktørId!!)

        val periodeOvergangsstønadTom = LocalDate.now().minusMonths(3)
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = listOf(
                EksternPeriode(
                    personIdent = søkersAktør.aktivFødselsnummer(),
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = periodeOvergangsstønadTom,
                    datakilde = Datakilde.EF,
                ),
            ),
        )
        autovedtakStegService.kjørBehandlingSmåbarnstillegg(
            mottakersAktør = søkersAktør,
            aktør = søkersAktør,
        )

        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak!!.id)!!

        // Vedtaksperioder skal være slettet etter at den er blitt omgjort til manuell behandling
        assertEquals(
            0,
            vedtaksperiodeService.hentPersisterteVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = aktivBehandling.id),
            ).size,
        )

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = aktivBehandling.id,
                beskrivelse = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt",
                manuellOppgaveType = ManuellOppgaveType.SMÅBARNSTILLEGG,
            )
        }

        assertEquals(StegType.BEHANDLINGSRESULTAT, aktivBehandling.steg)
        assertEquals(BehandlingStatus.UTREDES, aktivBehandling.status)

        val behandlingEtterHenleggelse = stegService.håndterHenleggBehandling(
            behandling = aktivBehandling,
            henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                begrunnelse = "",
            ),
        )
        assertEquals(false, behandlingEtterHenleggelse.aktiv)
    }

    @Test
    @Order(4)
    fun `Skal automatisk endre småbarnstilleggperioder`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)

        val søkersIdent = scenario.søker.ident!!
        val søkersAktør = personidentService.hentAktør(søkersIdent)

        val periodeOvergangsstønadTom = LocalDate.now()
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = listOf(
                EksternPeriode(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = periodeOvergangsstønadTom,
                    datakilde = Datakilde.EF,
                ),
            ),
        )
        autovedtakStegService.kjørBehandlingSmåbarnstillegg(
            mottakersAktør = søkersAktør,
            aktør = søkersAktør,
        )

        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak!!.id)!!

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = aktivBehandling.id,
            )
        val småbarnstilleggAndel = andelerTilkjentYtelse.single { it.type == YtelseType.SMÅBARNSTILLEGG }
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndel.stønadFom,
        )
        assertEquals(
            periodeOvergangsstønadTom.toYearMonth(),
            småbarnstilleggAndel.stønadTom,
        )

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = aktivBehandling.id),
        )

        val aktuellVedtaksperiode =
            vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == YearMonth.now().nesteMåned() }
        assertNotNull(aktuellVedtaksperiode)
        assertTrue(aktuellVedtaksperiode?.begrunnelser?.any { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD } == true)

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )
    }
}
