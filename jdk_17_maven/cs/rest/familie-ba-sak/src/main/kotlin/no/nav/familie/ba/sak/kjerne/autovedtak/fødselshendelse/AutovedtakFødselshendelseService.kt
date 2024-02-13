package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.FødselshendelseData
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.søker
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AutovedtakFødselshendelseService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val filtreringsreglerService: FiltreringsreglerService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val stegService: StegService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService,
    private val personopplysningerService: PersonopplysningerService,
    private val statsborgerskapService: StatsborgerskapService,
    private val opprettTaskService: OpprettTaskService,
    private val oppgaveService: OppgaveService,
) : AutovedtakBehandlingService<FødselshendelseData> {

    val stansetIAutomatiskFiltreringCounter =
        Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingCounter =
        Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringOgVilkårsvurderingCounter = Metrics.counter("familie.ba.sak.henvendelse.passert")

    override fun skalAutovedtakBehandles(behandlingsdata: FødselshendelseData): Boolean {
        val nyBehandlingHendelse = behandlingsdata.nyBehandlingHendelse
        val morsAktør = personidentService.hentAktør(nyBehandlingHendelse.morsIdent)
        val morsÅpneBehandling = hentÅpenNormalBehandling(aktør = morsAktør)
        val barnsAktører = personidentService.hentAktørIder(nyBehandlingHendelse.barnasIdenter)

        if (morsÅpneBehandling != null) {
            val barnaPåÅpenBehandling =
                persongrunnlagService.hentBarna(behandling = morsÅpneBehandling).map { it.aktør }

            if (barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                    barnaPåHendelse = barnsAktører,
                    barnaPåÅpenBehandling = barnaPåÅpenBehandling,
                )
            ) {
                logger.info("Ignorerer fødselshendelse fordi åpen behandling inneholder alle barna i hendelsen.")
                secureLogger.info(
                    "Ignorerer fødselshendelse fordi åpen behandling inneholder alle barna i hendelsen." +
                        "Barn på hendelse=${nyBehandlingHendelse.barnasIdenter}, barn på åpen behandling=$barnaPåÅpenBehandling",
                )
                return false
            }
        }

        val (barnSomSkalBehandlesForMor, alleBarnSomKanBehandles) = finnBarnSomSkalBehandlesForMor(
            fagsak = fagsakService.hentNormalFagsak(aktør = morsAktør),
            nyBehandlingHendelse = nyBehandlingHendelse,
        )

        if (barnSomSkalBehandlesForMor.isEmpty()) {
            logger.info("Ignorere fødselshendelse, alle barna fra hendelse er allerede behandlet")
            secureLogger.info(
                "Ignorere fødselshendelse, alle barna fra hendelse er allerede behandlet. " +
                    "Alle barna som kan behandles=$alleBarnSomKanBehandles, ",
            )
            return false
        }

        return true
    }

    override fun kjørBehandling(behandlingsdata: FødselshendelseData): String {
        val nyBehandling = behandlingsdata.nyBehandlingHendelse
        val morsAktør = personidentService.hentAktør(nyBehandling.morsIdent)

        val (barnSomSkalBehandlesForMor, _) = finnBarnSomSkalBehandlesForMor(
            fagsak = fagsakService.hentNormalFagsak(aktør = morsAktør),
            nyBehandlingHendelse = nyBehandling,
        )

        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(
            nyBehandling.copy(
                barnasIdenter = barnSomSkalBehandlesForMor,
            ),
        )

        val behandlingEtterFiltrering =
            stegService.håndterFiltreringsreglerForFødselshendelser(behandling, nyBehandling)

        return if (behandlingEtterFiltrering.steg == StegType.HENLEGG_BEHANDLING) {
            stansetIAutomatiskFiltreringCounter.increment()

            henleggBehandlingOgOpprettManuellOppgave(
                behandling = behandlingEtterFiltrering,
                begrunnelse = filtreringsreglerService.hentFødselshendelsefiltreringResultater(behandlingId = behandling.id)
                    .first { it.resultat == Resultat.IKKE_OPPFYLT }.begrunnelse,
            )
        } else {
            vurderVilkår(behandling = behandlingEtterFiltrering, barnaSomVurderes = barnSomSkalBehandlesForMor)
        }
    }

    private fun vurderVilkår(behandling: Behandling, barnaSomVurderes: List<String>): String {
        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling = behandling)

        return if (behandlingEtterVilkårsvurdering.resultat == Behandlingsresultat.INNVILGET) {
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
            vedtaksperiodeService.oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak, barnaSomVurderes)

            val vedtakEtterToTrinn =
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling = behandlingEtterVilkårsvurdering)

            val task = IverksettMotOppdragTask.opprettTask(
                behandling,
                vedtakEtterToTrinn,
                SikkerhetContext.hentSaksbehandler(),
            )
            taskRepository.save(task)

            opprettFremleggsoppgaveDersomEØSMedlem(behandling)

            passertFiltreringOgVilkårsvurderingCounter.increment()

            AutovedtakStegService.BEHANDLING_FERDIG
        } else {
            stansetIAutomatiskVilkårsvurderingCounter

            henleggBehandlingOgOpprettManuellOppgave(behandling = behandlingEtterVilkårsvurdering)
        }
    }

    internal fun opprettFremleggsoppgaveDersomEØSMedlem(behandling: Behandling) {
        val gjeldendeStatsborgerskap =
            personopplysningerService.hentGjeldendeStatsborgerskap(behandling.fagsak.aktør)
        val medlemskap = statsborgerskapService.hentSterkesteMedlemskap(statsborgerskap = gjeldendeStatsborgerskap)
        if (medlemskap == Medlemskap.EØS) {
            logger.info("Oppretter task for opprettelse av fremleggsoppgave på $behandling")
            opprettTaskService.opprettOppgaveTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.Fremlegg,
                beskrivelse = "Kontroller gyldig opphold",
                fristForFerdigstillelse = LocalDate.now().plusYears(1),
            )
        }
    }

    private fun hentÅpenNormalBehandling(aktør: Aktør): Behandling? {
        return fagsakService.hentNormalFagsak(aktør)?.let {
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(it.id)
        }
    }

    private fun finnBarnSomSkalBehandlesForMor(
        fagsak: Fagsak?,
        nyBehandlingHendelse: NyBehandlingHendelse,
    ): Pair<List<String>, List<String>> {
        val morsAktør = personidentService.hentAktør(nyBehandlingHendelse.morsIdent)
        val barnaTilMor = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
            aktør = morsAktør,
        ).forelderBarnRelasjon.filter { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN }

        val barnaSomHarBlittBehandlet =
            if (fagsak != null) {
                behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsak.id).filter { !it.erHenlagt() }
                    .flatMap {
                        persongrunnlagService.hentBarna(behandling = it).map { barn -> barn.aktør.aktivFødselsnummer() }
                    }.distinct()
            } else {
                emptyList()
            }

        return finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = nyBehandlingHendelse,
            barnaTilMor = barnaTilMor,
            barnaSomHarBlittBehandlet = barnaSomHarBlittBehandlet,
            secureLogger = secureLogger,
        )
    }

    private fun henleggBehandlingOgOpprettManuellOppgave(
        behandling: Behandling,
        begrunnelse: String = "",
    ): String {
        val begrunnelseForManuellOppgave = if (begrunnelse == "") {
            hentBegrunnelseFraVilkårsvurdering(behandlingId = behandling.id)
        } else {
            begrunnelse
        }

        stegService.håndterHenleggBehandling(
            behandling = behandling,
            henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FØDSELSHENDELSE_UGYLDIG_UTFALL,
                begrunnelse = begrunnelseForManuellOppgave,
            ),
        )

        oppgaveService.opprettOppgaveForManuellBehandling(
            behandling = behandling,
            begrunnelse = "Fødselshendelse: $begrunnelseForManuellOppgave",
            manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
        )

        return "Henlegger behandling $behandling automatisk på grunn av ugyldig resultat"
    }

    private fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val søker = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id).søker()
        val søkerResultat = vilkårsvurdering?.personResultater?.find { it.aktør == søker.aktør }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        val lovligOppholdResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
        if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT && bosattIRiketResultat.evalueringÅrsaker.any {
                VilkårIkkeOppfyltÅrsak.valueOf(
                    it,
                ) == VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET_FLERE_ADRESSER_UTEN_FOM
            }
        ) {
            return "Mor har flere bostedsadresser uten fra- og med dato"
        } else if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT) {
            return "Mor er ikke bosatt i riket."
        } else if (lovligOppholdResultat?.resultat != Resultat.OPPFYLT) {
            return lovligOppholdResultat?.evalueringÅrsaker?.joinToString("\n") {
                when (lovligOppholdResultat.resultat) {
                    Resultat.IKKE_OPPFYLT -> VilkårIkkeOppfyltÅrsak.valueOf(it).beskrivelse
                    Resultat.IKKE_VURDERT -> VilkårKanskjeOppfyltÅrsak.valueOf(it).beskrivelse
                    else -> ""
                }
            }
                ?: "Mor har ikke lovlig opphold"
        }

        persongrunnlagService.hentBarna(behandling).forEach { barn ->
            val vilkårsresultat =
                vilkårsvurdering.personResultater.find { it.aktør == barn.aktør }?.vilkårResultater

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er over 18 år."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er ikke bosatt med mor."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er gift."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er ikke bosatt i riket."
            }
        }

        logger.error("Fant ikke begrunnelse for at fødselshendelse ikke kunne automatisk behandles.")

        return ""
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)
    }
}
