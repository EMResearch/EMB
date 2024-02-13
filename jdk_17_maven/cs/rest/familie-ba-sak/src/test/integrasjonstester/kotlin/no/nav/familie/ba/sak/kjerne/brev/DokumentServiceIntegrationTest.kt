package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.TEST_PDF
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.byggMottakerdata
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class DokumentServiceIntegrationTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val dokumentService: DokumentService,

    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val brevService: BrevService,

    @Autowired
    private val integrasjonClient: IntegrasjonClient,

    @Autowired
    private val arbeidsfordelingService: ArbeidsfordelingService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val brevKlient: BrevKlient,

    @Autowired
    private val dokumentGenereringService: DokumentGenereringService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val brevmalService: BrevmalService,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Hent vedtaksbrev`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VURDER_TILBAKEKREVING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligSaksbehandler",
            "saksbehandlerId",
        )
        totrinnskontrollService.besluttTotrinnskontroll(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligBeslutter",
            "beslutterId",
            Beslutning.GODKJENT,
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrevRess = dokumentService.hentBrevForVedtak(vedtak)
        assertEquals(Ressurs.Status.SUKSESS, pdfvedtaksbrevRess.status)
        assert(pdfvedtaksbrevRess.data!!.contentEquals(TEST_PDF))
    }

    @Test
    fun `Skal generere vedtaksbrev`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VURDER_TILBAKEKREVING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligSaksbehandler",
            "saksbehandlerId",
        )
        totrinnskontrollService.besluttTotrinnskontroll(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligBeslutter",
            "beslutterId",
            Beslutning.GODKJENT,
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrev = dokumentGenereringService.genererBrevForVedtak(vedtak)
        assert(pdfvedtaksbrev.contentEquals(TEST_PDF))
    }

    @Test
    fun `Skal verifisere at brev får riktig signatur ved alle steg i behandling`() {
        val mockSaksbehandler = "Mock Saksbehandler"
        val mockSaksbehandlerId = "mock.saksbehandler@nav.no"
        val mockBeslutter = "Mock Beslutter"
        val mockBeslutterId = "mock.beslutter@nav.no"

        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VURDER_TILBAKEKREVING,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!

        val vedtaksbrevFellesFelter = brevService.lagVedtaksbrevFellesfelter(vedtak)

        assertEquals("NAV Familie- og pensjonsytelser Oslo 1", vedtaksbrevFellesFelter.enhet)
        assertEquals("System", vedtaksbrevFellesFelter.saksbehandler)
        assertEquals("Beslutter", vedtaksbrevFellesFelter.beslutter)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            mockSaksbehandler,
            mockSaksbehandlerId,
        )
        val behandlingEtterSendTilBeslutter =
            behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandlingEtterSendTilBeslutter)

        val vedtakEtterSendTilBeslutter =
            vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterSendTilBeslutter.id)!!

        val vedtaksbrevFellesFelterEtterSendTilBeslutter =
            brevService.lagVedtaksbrevFellesfelter(vedtakEtterSendTilBeslutter)

        assertEquals(mockSaksbehandler, vedtaksbrevFellesFelterEtterSendTilBeslutter.saksbehandler)
        assertEquals("System", vedtaksbrevFellesFelterEtterSendTilBeslutter.beslutter)

        totrinnskontrollService.besluttTotrinnskontroll(
            behandling = behandlingEtterSendTilBeslutter,
            beslutter = mockBeslutter,
            beslutterId = mockBeslutterId,
            beslutning = Beslutning.GODKJENT,
        )
        val behandlingEtterVedtakBesluttet =
            behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.IVERKSETT_MOT_OPPDRAG)

        val vedtakEtterVedtakBesluttet =
            vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!

        val vedtaksbrevFellesFelterEtterVedtakBesluttet =
            brevService.lagVedtaksbrevFellesfelter(vedtakEtterVedtakBesluttet)

        assertEquals(mockSaksbehandler, vedtaksbrevFellesFelterEtterVedtakBesluttet.saksbehandler)
        assertEquals(mockBeslutter, vedtaksbrevFellesFelterEtterVedtakBesluttet.beslutter)
    }

    @Test
    fun `Skal verifisere at man ikke får generert brev etter at behandlingen er sendt fra beslutter`() {
        val behandlingEtterVedtakBesluttet = kjørStegprosessForFGB(
            tilSteg = StegType.BESLUTTE_VEDTAK,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!
        val feil = assertThrows<FunksjonellFeil> {
            dokumentGenereringService.genererBrevForVedtak(vedtak)
        }

        assert(
            feil.message!!.contains("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter"),
        )
    }

    @Test
    fun `Sjekk at send brev for trukket søknad ikke genererer forside`() {
        val fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn1Fnr, barn2Fnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barn1Fnr, barn2Fnr),
                søkerAktør = behandling.fagsak.aktør,
                barnAktør = barnAktør,
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val manueltBrevRequest = ManueltBrevRequest(
            brevmal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
            mottakerIdent = fnr,
        ).byggMottakerdata(
            behandling,
            persongrunnlagService,
            arbeidsfordelingService,
        )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 1) {
            integrasjonClient.journalførDokument(match { it.fnr == fnr })
        }
    }

    @Test
    fun `Test sending varsel om revurdering til institusjon`() {
        val fnr = "09121079074"
        val orgNummer = "998765432"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = fnr,
            fagsakType = FagsakType.INSTITUSJON,
            institusjon = InstitusjonInfo(orgNummer = orgNummer, tssEksternId = "8000000"),
        )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlagForInstitusjon(
            behandlingId = behandling.id,
            barnasIdenter = listOf(fnr),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest = ManueltBrevRequest(
            brevmal = Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON,
            mottakerIdent = orgNummer,
        ).byggMottakerdata(
            behandling,
            persongrunnlagService,
            arbeidsfordelingService,
        )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 1) {
            integrasjonClient.journalførDokument(match { it.fnr == fnr && it.avsenderMottaker?.id == orgNummer && it.avsenderMottaker?.navn == "Testinstitusjon" })
        }
        assertEquals(fnr, manueltBrevRequest.vedrørende?.fødselsnummer)
        assertEquals("institusjonsbarnets navn", manueltBrevRequest.vedrørende?.navn)
        verify {
            brevKlient.genererBrev(
                "bokmaal",
                match {
                    it.mal == Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON && it.data.flettefelter.gjelder!!.first() == "institusjonsbarnets navn" &&
                        it.data.flettefelter.organisasjonsnummer!!.first() == orgNummer
                },
            )
        }
    }

    @Test
    fun `Test sending innhent dokumentasjon til institusjon`() {
        val fnr = randomFnr()
        val orgNummer = "998765432"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = fnr,
            fagsakType = FagsakType.INSTITUSJON,
            institusjon = InstitusjonInfo(orgNummer = orgNummer, tssEksternId = "8000000"),
        )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlagForInstitusjon(
            behandlingId = behandling.id,
            barnasIdenter = listOf(fnr),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest = ManueltBrevRequest(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
            mottakerIdent = orgNummer,
        ).byggMottakerdata(
            behandling,
            persongrunnlagService,
            arbeidsfordelingService,
        )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 1) {
            integrasjonClient.journalførDokument(match { it.fnr == fnr && it.avsenderMottaker?.id == orgNummer && it.avsenderMottaker?.navn == "Testinstitusjon" })
        }
        assertEquals(fnr, manueltBrevRequest.vedrørende?.fødselsnummer)
        assertEquals("institusjonsbarnets navn", manueltBrevRequest.vedrørende?.navn)
    }

    @Test
    fun `Test sending svartidsbrev til institusjon`() {
        val fnr = "10121079074"
        val orgNummer = "998765432"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = fnr,
            fagsakType = FagsakType.INSTITUSJON,
            institusjon = InstitusjonInfo(orgNummer = orgNummer, tssEksternId = "8000000"),
        )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlagForInstitusjon(
            behandlingId = behandling.id,
            barnasIdenter = listOf(fnr),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest = ManueltBrevRequest(
            brevmal = Brevmal.SVARTIDSBREV_INSTITUSJON,
            mottakerIdent = orgNummer,
        ).byggMottakerdata(
            behandling,
            persongrunnlagService,
            arbeidsfordelingService,
        )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 1) {
            integrasjonClient.journalførDokument(match { it.fnr == fnr && it.avsenderMottaker?.id == orgNummer && it.avsenderMottaker?.navn == "Testinstitusjon" })
        }
        assertEquals(fnr, manueltBrevRequest.vedrørende?.fødselsnummer)
        assertEquals("institusjonsbarnets navn", manueltBrevRequest.vedrørende?.navn)
    }

    @Test
    fun `Test sending forlenget svartidsbrev til institusjon`() {
        val fnr = "11121079074"
        val orgNummer = "998765432"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = fnr,
            fagsakType = FagsakType.INSTITUSJON,
            institusjon = InstitusjonInfo(orgNummer = orgNummer, tssEksternId = "8000000"),
        )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlagForInstitusjon(
            behandlingId = behandling.id,
            barnasIdenter = listOf(fnr),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest = ManueltBrevRequest(
            brevmal = Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
            mottakerIdent = orgNummer,
            antallUkerSvarfrist = 3,
        ).byggMottakerdata(
            behandling,
            persongrunnlagService,
            arbeidsfordelingService,
        )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 1) {
            integrasjonClient.journalførDokument(match { it.fnr == fnr && it.avsenderMottaker?.id == orgNummer && it.avsenderMottaker?.navn == "Testinstitusjon" })
        }
        assertEquals(fnr, manueltBrevRequest.vedrørende?.fødselsnummer)
        assertEquals("institusjonsbarnets navn", manueltBrevRequest.vedrørende?.navn)
    }

    fun lagTestPersonopplysningGrunnlagForInstitusjon(
        behandlingId: Long,
        barnasIdenter: List<String>,
        barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
        barnAktør: List<Aktør> = barnasIdenter.map { fødselsnummer ->
            tilAktør(fødselsnummer).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = fødselsnummer,
                        aktør = it,
                        aktiv = fødselsnummer == it.personidenter.first().fødselsnummer,
                    ),
                )
            }
        },
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
        val bostedsadresse = GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        )

        barnAktør.mapIndexed { index, aktør ->
            personopplysningGrunnlag.personer.add(
                Person(
                    aktør = aktør,
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = barnasFødselsdatoer[index],
                    navn = "institusjonsbarnets navn",
                    kjønn = Kjønn.MANN,
                ).also { barn ->
                    barn.statsborgerskap = mutableListOf(
                        GrStatsborgerskap(
                            landkode = "NOR",
                            medlemskap = Medlemskap.NORDEN,
                            person = barn,
                        ),
                    )
                    barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                    barn.sivilstander = mutableListOf(
                        GrSivilstand(
                            type = SIVILSTAND.UGIFT,
                            person = barn,
                        ),
                    )
                },
            )
        }
        return personopplysningGrunnlag
    }
}
