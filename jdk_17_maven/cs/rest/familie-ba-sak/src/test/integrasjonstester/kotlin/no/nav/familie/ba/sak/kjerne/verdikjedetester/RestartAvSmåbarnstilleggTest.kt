package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.time.YearMonth

class RestartAvSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val featureToggleService: FeatureToggleService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractVerdikjedetest() {

    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal finne alle fagsaker hvor småbarnstillegg starter opp igjen inneværende måned, og ikke er begrunnet`() {
        val restartSmåbarnstilleggMåned = LocalDate.now().plusMonths(4)

        // Fagsak 1 - har åpen behandling og skal ikke tas med
        val personScenario1: RestScenario = lagScenario(barnFødselsdato)
        val fagsak1: RestMinimalFagsak = lagFagsak(personScenario = personScenario1)
        fullførBehandling(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )

        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                EksternPeriode(
                    personIdent = personScenario1.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
                EksternPeriode(
                    personIdent = personScenario1.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )
        startEnRevurderingNyeOpplysningerMenIkkeFullfør(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )

        // Fagsak 2 - har restart av småbarnstillegg som ikke er begrunnet og skal være med i listen
        val personScenario2: RestScenario = lagScenario(barnFødselsdato)
        val fagsak2: RestMinimalFagsak = lagFagsak(personScenario = personScenario2)
        fullførBehandling(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                EksternPeriode(
                    personIdent = personScenario2.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
                EksternPeriode(
                    personIdent = personScenario2.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )

        // Fagsak 3 - har restart av småbarnstillegg som allerede er begrunnet, skal ikke være med i listen
        val personScenario3: RestScenario = lagScenario(barnFødselsdato)
        val fagsak3: RestMinimalFagsak = lagFagsak(personScenario = personScenario3)
        fullførBehandling(
            fagsak = fagsak3,
            personScenario = personScenario3,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak3,
            personScenario = personScenario3,
            barnFødselsdato = barnFødselsdato,
            skalBegrunneSmåbarnstillegg = true,
            mockPerioderMedOvergangsstønad = listOf(
                EksternPeriode(
                    personIdent = personScenario3.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
                EksternPeriode(
                    personIdent = personScenario3.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )

        val fagsaker: List<Long> =
            restartAvSmåbarnstilleggService.finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned = restartSmåbarnstilleggMåned.toYearMonth())

        Assertions.assertTrue(fagsaker.containsAll(listOf(fagsak2.id)))
        Assertions.assertFalse(fagsaker.contains(fagsak1.id))
        Assertions.assertFalse(fagsaker.contains(fagsak3.id))
    }

    @Test
    fun `Skal finne en fagsak hvor småbarnstillegg starter opp igjen inneværende måned selv om det er utført satsendring`() {
        val satsendringDato = SatsService.finnSisteSatsFor(SatsType.SMA).gyldigFom.toYearMonth()

        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(
            2022,
            12,
            1,
        ) // Mocker slik at behandling får gammel sats

        // Fagsak - har restart dato på samme dato som satsendringen
        val personScenario: RestScenario = lagScenario(barnFødselsdato)
        val fagsakMedSatsendringOgSmåbarnstilleggSomSkalRestartes: RestMinimalFagsak =
            lagFagsak(personScenario = personScenario)
        fullførBehandling(
            fagsak = fagsakMedSatsendringOgSmåbarnstilleggSomSkalRestartes,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsakMedSatsendringOgSmåbarnstilleggSomSkalRestartes,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                EksternPeriode(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = satsendringDato.minusMonths(2).sisteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
                EksternPeriode(
                    personIdent = personScenario.søker.ident,
                    fomDato = satsendringDato.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )

        unmockkObject(SatsTidspunkt)

        // satsendring gjør at den får en ny andel på småbarnstillegg med gyldig fom satsendringsdatoen
        fullførSatsendring(fagsakMedSatsendringOgSmåbarnstilleggSomSkalRestartes.id, satsendringDato)

        val fagsaker: List<Long> =
            restartAvSmåbarnstilleggService.finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned = satsendringDato)

        Assertions.assertTrue(fagsaker.contains(fagsakMedSatsendringOgSmåbarnstilleggSomSkalRestartes.id))
    }

    @Test
    fun `Satsendring skal ikke restarte småbarnstillegg på allerede løpende småbarnstillegg`() {
        val satsendringDato = SatsService.finnSisteSatsFor(SatsType.SMA).gyldigFom.toYearMonth()

        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(
            2022,
            12,
            1,
        ) // Mocker slik at behandling får gammel sats

        // Fagsak  - har løpende fagsak med småbarnstillegg og skal ikke restartes
        val personScenario2: RestScenario = lagScenario(barnFødselsdato)
        val fagsakMedSatsendringOgSmåbarnstilleggSomIkkeSkalRestartes: RestMinimalFagsak =
            lagFagsak(personScenario = personScenario2)
        fullførBehandling(
            fagsak = fagsakMedSatsendringOgSmåbarnstilleggSomIkkeSkalRestartes,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsakMedSatsendringOgSmåbarnstilleggSomIkkeSkalRestartes,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                EksternPeriode(
                    personIdent = personScenario2.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )

        // satsendring gjør at den får en ny andel på småbarnstillegg med gyldig fom satsendringsdatoen
        fullførSatsendring(fagsakMedSatsendringOgSmåbarnstilleggSomIkkeSkalRestartes.id, satsendringDato)

        val fagsaker: List<Long> =
            restartAvSmåbarnstilleggService.finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned = satsendringDato)

        Assertions.assertFalse(fagsaker.contains(fagsakMedSatsendringOgSmåbarnstilleggSomIkkeSkalRestartes.id))
    }

    fun lagScenario(barnFødselsdato: LocalDate): RestScenario = mockServerKlient().lagScenario(
        RestScenario(
            søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
            barna = listOf(
                RestScenarioPerson(
                    fødselsdato = barnFødselsdato.toString(),
                    fornavn = "Barn",
                    etternavn = "Barnesen",
                    bostedsadresser = emptyList(),
                ),
            ),
        ),
    )

    fun lagFagsak(personScenario: RestScenario): RestMinimalFagsak {
        return familieBaSakKlient().opprettFagsak(søkersIdent = personScenario.søker.ident!!).data!!
    }

    fun fullførBehandling(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {
        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = emptyList(),
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )
        val behandling = behandlingHentOgPersisterService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = fagsak.søkerFødselsnummer,
                    barnasIdenter = personScenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET,
                ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        return fullførRestenAvBehandlingen(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
        )
    }

    fun fullførRevurderingMedOvergangstonad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
        mockPerioderMedOvergangsstønad: List<EksternPeriode> = listOf(
            EksternPeriode(
                personIdent = personScenario.søker.ident!!,
                fomDato = barnFødselsdato.plusYears(1),
                tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                datakilde = Datakilde.EF,
            ),
        ),
        skalBegrunneSmåbarnstillegg: Boolean = false,
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = mockPerioderMedOvergangsstønad,
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )

        return fullførRestenAvBehandlingen(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
            skalBegrunneSmåbarnstillegg = skalBegrunneSmåbarnstillegg,
        )
    }

    private fun fullførSatsendring(fagsakId: Long, satsendringsTidspunkt: YearMonth) {
        unmockkObject(SatsTidspunkt)
        autovedtakSatsendringService.kjørBehandling(SatsendringTaskDto(fagsakId, satsendringsTidspunkt))
        val satsendring = behandlingHentOgPersisterService.hentBehandlinger(fagsakId).first { it.erSatsendring() }

        val iverksattBehandling = håndterIverksettingAvBehandling(
            behandlingEtterVurdering = satsendring,
            søkerFnr = satsendring.fagsak.aktør.aktivFødselsnummer(),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )

        if (!iverksattBehandling.erVedtatt() && iverksattBehandling.aktiv) error("Satsendringen er ikke utført $iverksattBehandling")
    }

    fun settAlleVilkårTilOppfylt(restUtvidetBehandling: RestUtvidetBehandling, barnFødselsdato: LocalDate) {
        restUtvidetBehandling.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.behandlingId,
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
    }

    private fun startEnRevurderingNyeOpplysningerMenIkkeFullfør(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = listOf(
                EksternPeriode(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = Datakilde.EF,
                ),
            ),
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )
        return behandlingHentOgPersisterService.hent(restUtvidetBehandling.data!!.behandlingId)
    }

    fun fullførRestenAvBehandlingen(
        restUtvidetBehandling: RestUtvidetBehandling,
        personScenario: RestScenario,
        fagsak: RestMinimalFagsak,
        skalBegrunneSmåbarnstillegg: Boolean = false,
    ): Behandling {
        settAlleVilkårTilOppfylt(
            restUtvidetBehandling = restUtvidetBehandling,
            barnFødselsdato = barnFødselsdato,
        )

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.behandlingId,
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
        )

        val utvidetVedtaksperiodeMedBegrunnelser = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelser.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = utvidetVedtaksperiodeMedBegrunnelser.gyldigeBegrunnelser.filter(String::isNotEmpty),
            ),
        )
        if (skalBegrunneSmåbarnstillegg) {
            val småbarnstilleggVedtaksperioder =
                vedtaksperioderMedBegrunnelser.filter {
                    it.utbetalingsperiodeDetaljer.filter { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.ytelseType == YtelseType.SMÅBARNSTILLEGG }
                        .isNotEmpty()
                }

            småbarnstilleggVedtaksperioder.forEach { periode ->
                familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
                    vedtaksperiodeId = periode.id,
                    restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                        standardbegrunnelser = listOf(
                            Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG.enumnavnTilString(),
                        ),
                    ),
                )
            }
        }

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

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
        return håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = personScenario.søker.ident!!,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )
    }
}
