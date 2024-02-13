package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class FiltreringsreglerService(
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val localDateService: LocalDateService,
    private val fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository,
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        Filtreringsregel.values().map {
            Resultat.values().forEach { resultat ->
                filtreringsreglerMetrics["${it.name}_${resultat.name}"] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.utfall",
                        "beskrivelse",
                        it.name,
                        "resultat",
                        resultat.name,
                    )

                filtreringsreglerFørsteUtfallMetrics[it.name] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.foersteutfall",
                        "beskrivelse",
                        it.name,
                    )
            }
        }
    }

    fun lagreFiltreringsregler(
        evalueringer: List<Evaluering>,
        behandlingId: Long,
        fakta: FiltreringsreglerFakta,
    ): List<FødselshendelsefiltreringResultat> {
        return fødselshendelsefiltreringResultatRepository.saveAll(
            evalueringer.map {
                FødselshendelsefiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.valueOf(it.identifikator),
                    resultat = it.resultat,
                    begrunnelse = it.begrunnelse,
                    evalueringsårsaker = it.evalueringÅrsaker.map { evalueringÅrsak -> evalueringÅrsak.toString() },
                    regelInput = fakta.convertDataClassToJson(),
                )
            },
        )
    }

    fun hentFødselshendelsefiltreringResultater(behandlingId: Long): List<FødselshendelsefiltreringResultat> {
        return fødselshendelsefiltreringResultatRepository.finnFødselshendelsefiltreringResultater(behandlingId = behandlingId)
    }

    fun kjørFiltreringsregler(
        nyBehandlingHendelse: NyBehandlingHendelse,
        behandling: Behandling,
    ): List<FødselshendelsefiltreringResultat> {
        val morsAktørId = personidentService.hentAktør(nyBehandlingHendelse.morsIdent)
        val barnasAktørId = personidentService.hentAktørIder(nyBehandlingHendelse.barnasIdenter)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasAktørId.contains(it.aktør) }

        val migreringsdatoPåFagsak = behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id)

        val sisteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)
        val andelerPåSisteBehandling = sisteBehandling?.let {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id)
        } ?: emptyList()
        val sisteMånedMedBarnetrygd = andelerPåSisteBehandling.maxOfOrNull { it.stønadTom }
        val harAndelerFremoverITid = sisteMånedMedBarnetrygd != null && sisteMånedMedBarnetrygd > YearMonth.now()

        val fakta = FiltreringsreglerFakta(
            mor = personopplysningGrunnlag.søker,
            morMottarLøpendeUtvidet = behandling.underkategori == BehandlingUnderkategori.UTVIDET,
            morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
                behandling,
                barnaFraHendelse,
            ),
            morMottarEøsBarnetrygd = behandling.kategori == BehandlingKategori.EØS,
            barnaFraHendelse = barnaFraHendelse,
            restenAvBarna = finnRestenAvBarnasPersonInfo(morsAktørId, barnaFraHendelse),
            morLever = !personopplysningGrunnlag.søker.erDød(),
            barnaLever = personopplysningGrunnlag.barna.none { it.erDød() },
            morHarVerge = personopplysningerService.harVerge(morsAktørId).harVerge,
            dagensDato = localDateService.now(),
            erFagsakenMigrertEtterBarnFødt = erSakenMigrertEtterBarnFødt(
                barnaFraHendelse,
                migreringsdatoPåFagsak,
            ),
            løperBarnetrygdForBarnetPåAnnenForelder = tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling = behandling,
                barna = barnaFraHendelse,
            ),
            morHarIkkeOpphørtBarnetrygd = andelerPåSisteBehandling.isEmpty() || harAndelerFremoverITid,
        )
        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(fakta)
        oppdaterMetrikker(evalueringer)

        logger.info("Resultater fra filtreringsregler på behandling $behandling: ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        if (!evalueringer.erOppfylt()) {
            secureLogger.info("Resultater fra filtreringsregler på behandling $behandling: (Fakta: ${fakta.convertDataClassToJson()}): ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        }

        return lagreFiltreringsregler(
            evalueringer = evalueringer,
            behandlingId = behandling.id,
            fakta = fakta,
        )
    }

    private fun erSakenMigrertEtterBarnFødt(
        barnaFraHendelse: List<Person>,
        migreringsdatoForFagsak: LocalDate?,
    ): Boolean = migreringsdatoForFagsak?.isAfter(barnaFraHendelse.minOf { it.fødselsdato }) == true

    private fun finnRestenAvBarnasPersonInfo(morsAktørId: Aktør, barnaFraHendelse: List<Person>): List<PersonInfo> {
        return personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morsAktørId).forelderBarnRelasjon.filter {
            it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.aktør == it.aktør }
        }.map {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(it.aktør)
        }
    }

    private fun økTellereForFørsteUtfall(evaluering: Evaluering, førsteutfall: Boolean): Boolean {
        if (evaluering.resultat == Resultat.IKKE_OPPFYLT && førsteutfall) {
            filtreringsreglerFørsteUtfallMetrics[evaluering.identifikator]!!.increment()
            return false
        }
        return førsteutfall
    }

    private fun oppdaterMetrikker(evalueringer: List<Evaluering>) {
        var førsteutfall = true
        evalueringer.forEach {
            filtreringsreglerMetrics["${it.identifikator}_${it.resultat.name}"]!!.increment()
            førsteutfall = økTellereForFørsteUtfall(it, førsteutfall)
        }
    }

    private fun morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
        behandling: Behandling,
        barnaFraHendelse: List<Person>,
    ): Boolean {
        val forrigeVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        return forrigeVedtatteBehandling?.let { vedtattBehandling ->
            vilkårsvurderingRepository.findByBehandlingAndAktiv(vedtattBehandling.id)?.let { vilkårsvurdering ->
                vilkårsvurdering.personResultater.single { personResultat -> personResultat.erSøkersResultater() }.vilkårResultater.any { vilkårResultat ->
                    vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD && vilkårResultat.erOppfylt() && barnaFraHendelse.any { barnFraHendelse ->
                        vilkårResultat.periodeTom?.isAfter(barnFraHendelse.fødselsdato) ?: true &&
                            vilkårResultat.periodeFom!!.isBefore(barnFraHendelse.fødselsdato.plusYears(18))
                    }
                }
            } ?: false
        } ?: false
    }

    companion object {

        val logger = LoggerFactory.getLogger(FiltreringsreglerService::class.java)
    }
}
