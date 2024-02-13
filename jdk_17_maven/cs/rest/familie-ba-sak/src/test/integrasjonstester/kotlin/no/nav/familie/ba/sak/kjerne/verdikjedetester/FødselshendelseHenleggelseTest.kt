package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.defaultBostedsadresseHistorikk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.util.sisteTilleggOrdinærSats
import no.nav.familie.ba.sak.util.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.util.tilleggOrdinærSatsNesteMånedTilTester
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now

class FødselshendelseHenleggelseTest(
    @Autowired private val opprettTaskService: OpprettTaskService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val utvidetBehandlingService: UtvidetBehandlingService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val opprettOppgaveForManuellBehandlingTask: OpprettOppgaveTask,
) : AbstractVerdikjedetest() {

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal ikke starte behandling i ba-sak fordi det finnes saker i infotrygd (velg fagsystem)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1982-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                    infotrygdSaker = InfotrygdSøkResponse(
                        bruker = listOf(
                            lagInfotrygdSak(
                                sisteTilleggOrdinærSats(),
                                listOf("1234"),
                                "OR",
                                "OS",
                            ),
                        ),
                        barn = emptyList(),
                    ),
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
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
        assertNull(behandling)

        verify(exactly = 1) {
            opprettTaskService.opprettSendFeedTilInfotrygdTask(scenario.barna.map { it.ident!! })
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at søker er under 18 (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = now().minusYears(16).toString(),
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
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
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = behandling!!.id,
                beskrivelse = "Fødselshendelse: Mor er under 18 år.",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }

        val fagsak =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        assertEquals(0, automatiskVurdertBehandling.personResultater.size)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at søker har flere adresser uten fom-dato (vilkårsvurdering)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1993-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                    bostedsadresser = defaultBostedsadresseHistorikk + listOf(
                        Bostedsadresse(
                            angittFlyttedato = null,
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231",
                            ),
                        ),
                        Bostedsadresse(
                            angittFlyttedato = null,
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231",
                            ),
                        ),
                        Bostedsadresse(
                            angittFlyttedato = now(),
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231",
                            ),
                        ),
                    ),
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().toString(),
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
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = behandling!!.id,
                beskrivelse = "Fødselshendelse: Mor har flere bostedsadresser uten fra- og med dato",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn ikke er bosatt i riket og bor ikke med mor (vilkårsvurdering)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList(),
                    ),
                ),
            ),
        )

        val barnIdent = scenario.barna.first().ident!!
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = behandling!!.id,
                beskrivelse = "Fødselshendelse: Barnet (fødselsdato: ${
                    LocalDate.parse(scenario.barna.first().fødselsdato)
                        .tilKortString()
                }) er ikke bosatt med mor.",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }

        val fagsak =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        val borMedSøkerVikårForbarn =
            automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER }
        val bosattIRiketVikårForbarn =
            automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(Resultat.IKKE_OPPFYLT, borMedSøkerVikårForbarn?.resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, bosattIRiketVikårForbarn?.resultat)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at mor mottar utvidet barnetrygd (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = now().minusYears(26).førsteDagINesteMåned().plusDays(6).toString(),
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                    RestScenarioPerson(
                        fødselsdato = now().minusYears(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )

        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident!!,
            barnasIdenter = listOf(scenario.barna.last().ident!!),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            brevmalService = brevmalService,

        )

        assertEquals(BehandlingUnderkategori.UTVIDET, behandling.underkategori)
        assertEquals(
            tilleggOrdinærSatsNesteMånedTilTester().beløp + sisteUtvidetSatsTilTester(),
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id),
            ),
        )

        val revurdering = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        assertEquals(BehandlingUnderkategori.UTVIDET, revurdering?.underkategori)
        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, revurdering?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, revurdering?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = revurdering!!.id,
                beskrivelse = "Fødselshendelse: Mor mottar utvidet barnetrygd.",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at mor mottar EØS-barnetrygd (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = now().minusYears(26).førsteDagINesteMåned().plusDays(6).toString(),
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                    RestScenarioPerson(
                        fødselsdato = now().minusYears(2).toString(),
                        fornavn = "Barn2",
                        etternavn = "Barnesen",
                    ),
                ),
            ),
        )

        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident!!,
            barnasIdenter = listOf(scenario.barna.last().ident!!),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            brevmalService = brevmalService,

        )

        oppdaterRegelverkTilEøs(behandling)

        val revurdering = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,

        )

        assertEquals(BehandlingKategori.EØS, revurdering?.kategori)
        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, revurdering?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, revurdering?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = revurdering!!.id,
                beskrivelse = "Fødselshendelse: Mor har EØS-barnetrygd",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }

    @Test
    fun `Skal sende tredjelandsborgere fra Ukraina til manuel oppfølging (midlertidig regel for ukrainakonflikten)`() {
        val fødselsdato = "1993-01-12"
        val barnFødselsdato = LocalDate.now()
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = fødselsdato, fornavn = "Mor", etternavn = "Søker").copy(
                    statsborgerskap = listOf(
                        Statsborgerskap(
                            land = "UKR",
                            gyldigFraOgMed = LocalDate.parse(fødselsdato),
                            bekreftelsesdato = LocalDate.parse(fødselsdato),
                            gyldigTilOgMed = null,
                        ),
                    ),
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ).copy(
                        statsborgerskap = listOf(
                            Statsborgerskap(
                                land = "UKR",
                                gyldigFraOgMed = barnFødselsdato,
                                bekreftelsesdato = barnFødselsdato,
                                gyldigTilOgMed = null,
                            ),
                        ),
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

        )!!

        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = behandling.id,
                beskrivelse = "Fødselshendelse: ${VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_MÅ_VURDERE_LENGDEN_PÅ_OPPHOLDSTILLATELSEN.beskrivelse}",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }

    private fun oppdaterRegelverkTilEøs(behandling: Behandling) {
        vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!.apply {
            personResultater.first { !it.erSøkersResultater() }.apply {
                vilkårResultater.forEach {
                    it.vurderesEtter = Regelverk.EØS_FORORDNINGEN
                    it.periodeFom = now().minusMonths(1)
                }
            }
            vilkårsvurderingService.oppdater(this)
        }
    }
}
