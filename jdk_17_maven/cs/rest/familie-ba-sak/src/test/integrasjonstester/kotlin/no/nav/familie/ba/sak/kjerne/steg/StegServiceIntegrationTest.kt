package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class StegServiceIntegrationTest(
    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val oppgaveRepository: OppgaveRepository,

    @Autowired
    private val brevmalService: BrevmalService,

    @Autowired
    private val økonomiKlient: ØkonomiKlient,

) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
        ClientMocks.clearPdlMocks(mockPersonopplysningerService)
    }

    @Test
    fun `Skal sette default-verdier på gift-vilkår for barn`() {
        val søkerFnr = randomFnr()
        val barnFnr1 = ClientMocks.barnFnr[0]
        val barnFnr2 = ClientMocks.barnFnr[1]

        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.REGISTRERE_SØKNAD,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr1, barnFnr2),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertEquals(
            Resultat.OPPFYLT,
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr1 }.vilkårResultater
                .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.resultat,
        )
        assertEquals(
            Resultat.IKKE_VURDERT,
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr2 }.vilkårResultater
                .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.resultat,
        )
    }

    @Test
    fun `Skal kjøre gjennom alle steg med datageneratoren`() {
        val søkerFnr = randomFnr()
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        // Venter med å kjøre gjennom til avsluttet til brev er støttet for fortsatt innvilget.
        kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.SEND_TIL_BESLUTTER,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = behandling.fagsak.id,
            brevmalService = brevmalService,
        )
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "98765432110")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        assertEquals(FØRSTE_STEG, behandling.steg)

        assertThrows<FunksjonellFeil> {
            stegService.håndterVilkårsvurdering(behandling)
        }
    }

    @Test
    fun `Skal feile når man prøver å endre en avsluttet behandling`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "98765432110")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandling.status = BehandlingStatus.AVSLUTTET
        val feil = assertThrows<FunksjonellFeil> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
        assertEquals(
            "Behandling med id ${behandling.id} er avsluttet og stegprosessen kan ikke gjenåpnes",
            feil.message,
        )
    }

    @Test
    fun `Skal feile når man prøver å noe annet enn å beslutte behandling når den er på dette steget`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "98765432110")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        assertThrows<FunksjonellFeil> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
    }

    @Test
    fun `Skal feile når man prøver å kalle beslutning-steget med feil status på behandling`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "98765432110")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        assertThrows<FunksjonellFeil> {
            stegService.håndterBeslutningForVedtak(
                behandling,
                RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT, begrunnelse = null),
            )
        }
    }

    @Test
    fun `Underkjent beslutning setter steg tilbake til send til beslutter`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val søkerAktørId = personidentService.hentAktør(søkerFnr)

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                søkerAktørId,
                behandling,
                Resultat.OPPFYLT,
            ),
        )
        behandling.endretAv = "1234"
        assertEquals(FØRSTE_STEG, behandling.steg)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        stegService.håndterBeslutningForVedtak(
            behandling,
            RestBeslutningPåVedtak(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Feil"),
        )

        val behandlingEtterPersongrunnlagSteg = behandlingHentOgPersisterService.hent(behandlingId = behandling.id)
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterPersongrunnlagSteg.steg)
    }

    @Test
    fun `Henlegge før behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving()

        val henlagtBehandling = stegService.håndterHenleggBehandling(
            vilkårsvurdertBehandling,
            RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                begrunnelse = "",
            ),
        )
        assertTrue(
            henlagtBehandling.behandlingStegTilstand.firstOrNull {
                it.behandlingSteg == StegType.HENLEGG_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            } != null,
        )
        assertTrue(
            henlagtBehandling.behandlingStegTilstand.firstOrNull {
                it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            } != null,
        )

        assertEquals(StegType.BEHANDLING_AVSLUTTET, henlagtBehandling.steg)
    }

    @Test
    fun `Teknisk henleggelse med begrunnelse Satsendring skal beholde behandleSak-oppgaven åpen`() {
        val behandling = kjørGjennomStegInkludertVurderTilbakekreving()
        oppgaveRepository.saveAll(
            listOf(
                DbOppgave(behandling = behandling, type = Oppgavetype.Journalføring, gsakId = "1"),
                DbOppgave(behandling = behandling, type = Oppgavetype.BehandleSak, gsakId = "2"),
                DbOppgave(behandling = behandling, type = Oppgavetype.BehandleUnderkjentVedtak, gsakId = "3"),
            ),
        )
        val henlagtBehandling = stegService.håndterHenleggBehandling(
            behandling,
            RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                begrunnelse = "Satsendring",
            ),
        )
        assertEquals(StegType.BEHANDLING_AVSLUTTET, henlagtBehandling.steg)
        assertTrue {
            oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.BehandleSak }.isNotEmpty()
        }
        assertTrue {
            oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.BehandleUnderkjentVedtak }.isNotEmpty()
        }
        assertTrue {
            oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.Journalføring }.isEmpty()
        }
    }

    @Test
    fun `Henlegge etter behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving()
        stegService.håndterSendTilBeslutter(vilkårsvurdertBehandling, "1234")

        val behandlingEtterSendTilBeslutter =
            behandlingHentOgPersisterService.hent(behandlingId = vilkårsvurdertBehandling.id)

        assertThrows<FunksjonellFeil> {
            stegService.håndterHenleggBehandling(
                behandlingEtterSendTilBeslutter,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                    begrunnelse = "",
                ),
            )
        }
    }

    // I de fleste tilfeller vil det ikke være mulig å henlegge en behandling som har kommet forbi iverksett steget.
    // Disse vil bli stoppet i BehandlingStegController.
    @Test
    fun `Henlegge dersom behandling står på FERDIGSTILLE_BEHANDLING steget`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "98765432110")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                0,
                behandling,
                StegType.FERDIGSTILLE_BEHANDLING,
                BehandlingStegStatus.IKKE_UTFØRT,
            ),
        )

        val behandlingEtterHenleggelse = stegService.håndterHenleggBehandling(
            behandling,
            RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET, begrunnelse = ""),
        )

        assertThat(behandlingEtterHenleggelse.steg).isEqualTo(StegType.BEHANDLING_AVSLUTTET)
        assertThat(behandlingEtterHenleggelse.status).isEqualTo(BehandlingStatus.AVSLUTTET)
        assertThat(behandlingEtterHenleggelse.behandlingStegTilstand.any { it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT })
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering innenfor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = nyMigreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )
        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterBeslutterSteg.steg)
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering utenefor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 600.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = nyMigreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )
        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterBeslutterSteg.steg)
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering utenfor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 500.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = nyMigreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )

        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )

        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterSendTilBeslutterSteg.steg)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med avvik i simulering innenfor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = migreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater.first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBesultterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesultterSteg.steg)
        assertTrue {
            behandlingEtterBesultterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBesultterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med avvik i simulering utenfor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 300.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = migreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater.first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )

        assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutterSteg.steg)

        val behandlingEtterBesluttVedtakSteg = stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutterSteg,
            RestBeslutningPåVedtak(
                Beslutning.GODKJENT,
                "godkjent manuelt",
            ),
        )

        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesluttVedtakSteg.steg)

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med manuelle posteringer med avvik innenfor beløpsgrenser`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = migreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater.first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )

        assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutterSteg.steg)

        // Må manuelt godkjenne vedtak
        val behandlingEtterBesluttVedtakSteg = stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutterSteg,
            RestBeslutningPåVedtak(
                Beslutning.GODKJENT,
                "godkjent manuelt",
            ),
        )

        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesluttVedtakSteg.steg)

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for endre migreringsdato behandling og automatisk godkjenne totrinnskontroll`() {
        val simulertPosteringMock = listOf(
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
                erFeilkonto = null,
            ),
        )

        val simuleringMottakerMock = listOf(
            SimuleringMottaker(
                simulertPostering = simulertPosteringMock,
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "12345678910",
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)

        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]
        val barnasIdenter = listOf(barnFnr)

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = migreringsdato,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultatSteg,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "ignorer tilbakekreving",
            ),
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg = stegService.håndterSendTilBeslutter(
            behandlingEtterTilbakekrevingSteg,
            "1234",
        )

        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterSendTilBeslutterSteg.steg)

        assertTrue {
            behandlingEtterSendTilBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterSendTilBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    private fun kjørGjennomStegInkludertVurderTilbakekreving(): Behandling {
        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]

        return kjørStegprosessForFGB(
            tilSteg = StegType.VURDER_TILBAKEKREVING,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )
    }

    private fun assertMigreringsdato(migreringsdato: LocalDate, behandling: Behandling) {
        assertEquals(migreringsdato, behandlingService.hentMigreringsdatoIBehandling(behandling.id))
    }
}
