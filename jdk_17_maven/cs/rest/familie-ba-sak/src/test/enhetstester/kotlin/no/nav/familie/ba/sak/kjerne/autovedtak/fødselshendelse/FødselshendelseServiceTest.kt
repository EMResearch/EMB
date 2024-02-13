package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.config.IntegrasjonClientMock
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.FødselshendelseData
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class FødselshendelseServiceTest {
    val filtreringsreglerService = mockk<FiltreringsreglerService>()
    val taskRepository = mockk<TaskRepositoryWrapper>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    val persongrunnlagService = mockk<PersongrunnlagService>()
    val personidentService = mockk<PersonidentService>()
    val stegService = mockk<StegService>()
    val vedtakService = mockk<VedtakService>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val autovedtakService = mockk<AutovedtakService>()
    val personopplysningerService = mockk<PersonopplysningerService>()
    val opprettTaskService = mockk<OpprettTaskService>()
    val oppgaveService = mockk<OppgaveService>()

    val integrasjonClient = mockk<IntegrasjonClient>()
    val statsborgerskapService = StatsborgerskapService(
        integrasjonClient = integrasjonClient,
    )

    private val autovedtakFødselshendelseService = AutovedtakFødselshendelseService(
        fagsakService,
        behandlingHentOgPersisterService,
        filtreringsreglerService,
        taskRepository,
        vilkårsvurderingRepository,
        persongrunnlagService,
        personidentService,
        stegService,
        vedtakService,
        vedtaksperiodeService,
        autovedtakService,
        personopplysningerService,
        statsborgerskapService,
        opprettTaskService,
        oppgaveService,
    )

    @Test
    fun `Skal opprette fremleggsoppgave dersom søker er EØS medlem`() {
        every { personopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "POL",
            gyldigFraOgMed = LocalDate.now().minusMonths(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null,
        )
        every { integrasjonClient.hentAlleEØSLand() } returns IntegrasjonClientMock.hentKodeverkLand()
        every { opprettTaskService.opprettOppgaveTask(any(), any(), any(), any()) } just runs

        autovedtakFødselshendelseService.opprettFremleggsoppgaveDersomEØSMedlem(lagBehandling())

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = any(),
                oppgavetype = Oppgavetype.Fremlegg,
                beskrivelse = "Kontroller gyldig opphold",
                fristForFerdigstillelse = LocalDate.now().plusYears(1),
            )
        }
    }

    @Test
    fun `Skal ikke opprette fremleggsoppgave dersom søker er nordisk medlem`() {
        every { personopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "DNK",
            gyldigFraOgMed = LocalDate.now().minusMonths(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null,
        )
        every { integrasjonClient.hentAlleEØSLand() } returns IntegrasjonClientMock.hentKodeverkLand()
        every { opprettTaskService.opprettOppgaveTask(any(), any(), any(), any()) } just runs

        autovedtakFødselshendelseService.opprettFremleggsoppgaveDersomEØSMedlem(lagBehandling())

        verify(exactly = 0) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = any(),
                oppgavetype = Oppgavetype.Fremlegg,
                beskrivelse = "Kontroller gyldig opphold",
                fristForFerdigstillelse = LocalDate.now().plusYears(1),
            )
        }
    }

    @Test
    fun `Skal opprette manuell oppgave hvis resultat av fødselshendelse blir INNVILGET_OG_ENDRET`() {
        val søkerPerson = lagPerson(type = PersonType.SØKER)
        val fagsak = defaultFagsak(aktør = søkerPerson.aktør)

        val søkerAktør = fagsak.aktør
        val søker = søkerAktør.aktivFødselsnummer()
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1 = barn1Person.aktør.aktivFødselsnummer()
        val barn2Person = lagPerson(type = PersonType.BARN)
        val barn2 = barn2Person.aktør.aktivFødselsnummer()
        val nyBehandlingHendelse = NyBehandlingHendelse(søker, listOf(barn2))

        every { fagsakService.hentNormalFagsak(søkerAktør) } returns fagsak
        every {
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                søker,
                true,
                FagsakType.NORMAL,
                null,
            )
        } returns fagsak

        val forrigeBehandling = lagBehandling(
            fagsak = fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            resultat = Behandlingsresultat.OPPHØRT,
            status = BehandlingStatus.AVSLUTTET,
        )
        val nyBehandling = lagBehandling(
            fagsak,
            behandlingKategori = BehandlingKategori.NASJONAL,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            årsak = BehandlingÅrsak.FØDSELSHENDELSE,
            skalBehandlesAutomatisk = true,
        )
        every { behandlingHentOgPersisterService.hent(forrigeBehandling.id) } returns forrigeBehandling
        every { behandlingHentOgPersisterService.lagreEllerOppdater(forrigeBehandling) } returns forrigeBehandling
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) } returns listOf(forrigeBehandling)
        every { behandlingHentOgPersisterService.hent(nyBehandling.id) } returns nyBehandling
        every { stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandlingHendelse) } returns nyBehandling
        every {
            stegService.håndterFiltreringsreglerForFødselshendelser(
                nyBehandling,
                nyBehandlingHendelse,
            )
        } returns nyBehandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        every { stegService.håndterVilkårsvurdering(nyBehandling) } returns nyBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
            .leggTilBehandlingStegTilstand(StegType.IVERKSETT_MOT_OPPDRAG)
        every { stegService.håndterHenleggBehandling(any(), any()) } returns nyBehandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""
        every { persongrunnlagService.hentSøker(nyBehandling.id) } returns søkerPerson
        every { persongrunnlagService.hentBarna(nyBehandling) } returns listOf(barn1Person, barn2Person)

        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(nyBehandling.id) } returns listOf(
            barn1Person.tilPersonEnkel(),
            barn2Person.tilPersonEnkel(),
            søkerPerson.tilPersonEnkel(),
        )

        every { personidentService.hentAktør(søker) } returns søkerAktør
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerAktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, Month.JANUARY, 5),
            navn = "Mor Mocksen",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = setOf(
                ForelderBarnRelasjon(aktør = tilAktør(barn1), relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN),
                ForelderBarnRelasjon(aktør = tilAktør(barn2), relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN),
            ),
        )
        every { persongrunnlagService.hentBarna(forrigeBehandling) } returns listOf(
            barn1Person,
        )
        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(forrigeBehandling.id) } returns listOf(
            barn1Person.tilPersonEnkel(),
        )
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(nyBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            søkerPerson,
            listOf(barn1Person, barn2Person),
            nyBehandling,
            id = 1,
            mapOf(
                Pair(
                    barn1Person.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.IKKE_OPPFYLT,
                            behandlingId = nyBehandling.id,
                        ),
                    ),
                ),
            ),
        )

        autovedtakFødselshendelseService.kjørBehandling(FødselshendelseData(nyBehandlingHendelse))
        verify(exactly = 0) {
            opprettTaskService.opprettOppgaveForManuellBehandlingTask(
                behandlingId = any(),
                beskrivelse = "Fødselshendelse: Barnet (fødselsdato: ${barn1Person.fødselsdato.tilKortString()}) er ikke bosatt med mor.",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }
}
