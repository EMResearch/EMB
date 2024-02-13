package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.BehandlingValidering.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.filtrerUtKunNorskeBostedsadresser
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.INSTITUSJON
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.ArbeidsforholdService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PersongrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val statsborgerskapService: StatsborgerskapService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val arbeidsforholdService: ArbeidsforholdService,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {

    fun mapTilRestPersonMedStatsborgerskapLand(person: Person): RestPerson {
        val restPerson = person.tilRestPerson()
        restPerson.registerhistorikk?.statsborgerskap
            ?.forEach { lagret ->
                val landkode = lagret.verdi
                val land = statsborgerskapService.hentLand(landkode)
                lagret.verdi = if (land.lowercase().contains("uoppgitt")) "$land ($landkode)" else land.storForbokstav()
            }
        return restPerson
    }

    fun hentSøker(behandlingId: Long): Person {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!.søker
    }

    fun hentBarna(behandling: Behandling): List<Person> {
        return hentBarna(behandling.id)
    }

    fun hentSøkerOgBarnPåBehandlingThrows(behandlingId: Long): List<PersonEnkel> =
        hentSøkerOgBarnPåBehandling(behandlingId)
            ?: error("Finner ikke søker/barn på behandling=$behandlingId")

    fun hentSøkerOgBarnPåBehandling(behandlingId: Long): List<PersonEnkel>? =
        personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilAktiv(behandlingId)
            .takeIf { it.isNotEmpty() }

    fun hentSøkerOgBarnPåFagsak(fagsakId: Long): Set<PersonEnkel>? =
        personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(fagsakId)
            .takeIf { it.isNotEmpty() }

    fun hentBarna(behandlingId: Long): List<Person> = personopplysningGrunnlagRepository
        .findByBehandlingAndAktiv(behandlingId)!!.barna

    fun hentPersonerPåBehandling(identer: List<String>, behandling: Behandling): List<Person> {
        val aktørIder = personidentService.hentAktørIder(identer)

        val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw Feil("Finner ikke personopplysningsgrunnlag på behandling ${behandling.id}")
        return grunnlag.søkerOgBarn.filter { person -> aktørIder.contains(person.aktør) }
    }

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
    }

    fun hentAktivThrows(behandlingId: Long): PersonopplysningGrunnlag {
        return hentAktiv(behandlingId = behandlingId)
            ?: throw Feil("Finner ikke personopplysningsgrunnlag på behandling $behandlingId")
    }

    @Transactional
    fun oppdaterRegisteropplysninger(behandlingId: Long): PersonopplysningGrunnlag {
        val nåværendeGrunnlag = hentAktivThrows(behandlingId = behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)

        validerBehandlingKanRedigeres(behandling)

        return hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = nåværendeGrunnlag.søker.aktør,
            barnFraInneværendeBehandling = nåværendeGrunnlag.barna.map { it.aktør },
            behandling = behandling,
            målform = nåværendeGrunnlag.søker.målform,
        )
    }

    /**
     * Legger til barn i nytt personopplysningsgrunnlag
     */
    @Transactional
    fun leggTilBarnIPersonopplysningsgrunnlag(
        nyttBarnIdent: String,
        behandling: Behandling,
    ) {
        val nyttbarnAktør = personidentService.hentOgLagreAktør(nyttBarnIdent, true)

        val personopplysningGrunnlag = hentAktivThrows(behandlingId = behandling.id)

        val barnIGrunnlag = personopplysningGrunnlag.barna.map { it.aktør }

        if (barnIGrunnlag.contains(nyttbarnAktør)) {
            throw FunksjonellFeil(
                melding = "Forsøker å legge til barn som allerede finnes i personopplysningsgrunnlag ${personopplysningGrunnlag.id}",
                frontendFeilmelding = "Barn finnes allerede på behandling og er derfor ikke lagt til.",
            )
        }

        val oppdatertGrunnlag = hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = personopplysningGrunnlag.søker.aktør,
            barnFraInneværendeBehandling = barnIGrunnlag.plus(nyttbarnAktør).toList(),
            behandling = behandling,
            målform = personopplysningGrunnlag.søker.målform,
        )

        oppdatertGrunnlag.barna.singleOrNull { nyttbarnAktør == it.aktør }
            ?.also { loggService.opprettBarnLagtTilLogg(behandling, it) } ?: run {
            secureLogger.info("Klarte ikke legge til barn med aktør $nyttbarnAktør på personopplysningsgrunnlag ${personopplysningGrunnlag.id}")
            throw Feil("Nytt barn ikke lagt til i personopplysningsgrunnlag ${personopplysningGrunnlag.id}. Se securelog for mer informasjon.")
        }
    }

    fun finnNyeBarn(behandling: Behandling, forrigeBehandling: Behandling?): List<Person> {
        val barnIForrigeGrunnlag = forrigeBehandling?.let { hentAktiv(behandlingId = it.id)?.barna } ?: emptySet()
        val barnINyttGrunnlag = behandling.let { hentAktivThrows(behandlingId = it.id).barna }

        return barnINyttGrunnlag.filter { barn -> barnIForrigeGrunnlag.none { barn.aktør == it.aktør } }
    }

    /**
     * Registrerer barn valgt i søknad og barn fra forrige behandling
     */
    @Transactional
    fun registrerBarnFraSøknad(
        søknadDTO: SøknadDTO,
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling? = null,
    ) {
        val søkerAktør = personidentService.hentOgLagreAktør(søknadDTO.søkerMedOpplysninger.ident, true)
        val valgteBarnsAktør =
            søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }
                .map { barn -> personidentService.hentOgLagreAktør(barn.ident, true) }

        val barnMedTilkjentYtelseIForrigeBehandling =
            if (skalTaMedBarnFraForrigeBehandling(behandling) && forrigeBehandlingSomErVedtatt != null) {
                finnBarnMedTilkjentYtelseIBehandling(forrigeBehandlingSomErVedtatt)
            } else {
                emptyList()
            }

        hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = søkerAktør,
            barnFraInneværendeBehandling = valgteBarnsAktør,
            barnFraForrigeBehandling = barnMedTilkjentYtelseIForrigeBehandling,
            behandling = behandling,
            målform = søknadDTO.søkerMedOpplysninger.målform,
        )
    }

    private fun finnBarnMedTilkjentYtelseIBehandling(behandling: Behandling): List<Aktør> =
        hentAktiv(behandlingId = behandling.id)?.barna?.map { it.aktør }?.filter {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, it).isNotEmpty()
        } ?: emptyList()

    /**
     * Henter oppdatert registerdata og lagrer i nytt aktivt personopplysningsgrunnlag
     */
    @Transactional
    fun hentOgLagreSøkerOgBarnINyttGrunnlag(
        aktør: Aktør,
        barnFraInneværendeBehandling: List<Aktør>,
        behandling: Behandling,
        målform: Målform,
        barnFraForrigeBehandling: List<Aktør> = emptyList(),
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val enkelPersonInfo = behandling.erMigrering() || behandling.erSatsendring()
        val søker = hentPerson(
            aktør = aktør,
            personopplysningGrunnlag = personopplysningGrunnlag,
            målform = målform,
            personType = when (behandling.fagsak.type) {
                NORMAL -> PersonType.SØKER
                BARN_ENSLIG_MINDREÅRIG, INSTITUSJON -> PersonType.BARN
            },
            enkelPersonInfo = enkelPersonInfo,
            hentArbeidsforhold = behandling.skalBehandlesAutomatisk,
        )
        personopplysningGrunnlag.personer.add(søker)

        barnFraInneværendeBehandling.union(barnFraForrigeBehandling).forEach { barnsAktør ->
            personopplysningGrunnlag.personer.add(
                hentPerson(
                    aktør = barnsAktør,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    målform = målform,
                    personType = PersonType.BARN,
                    enkelPersonInfo = enkelPersonInfo,
                ),
            )
        }

        if (søker.hentSterkesteMedlemskap() == Medlemskap.EØS && behandling.skalBehandlesAutomatisk) {
            hentFarEllerMedmorAktør(barnFraInneværendeBehandling)?.also { farEllerMedmor ->
                personopplysningGrunnlag.personer.add(
                    hentPerson(
                        aktør = farEllerMedmor,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                        målform = målform,
                        personType = PersonType.ANNENPART,
                        enkelPersonInfo = enkelPersonInfo,
                        hentArbeidsforhold = true,
                    ),
                )
            }
        }

        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag).also {
            /**
             * For sikkerhetsskyld fastsetter vi alltid behandlende enhet når nytt personopplysningsgrunnlag opprettes.
             * Dette gjør vi fordi det kan ha blitt introdusert personer med fortrolig adresse.
             */
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling)
            saksstatistikkEventPublisher.publiserSaksstatistikk(behandling.fagsak.id)
        }
    }

    private fun hentPerson(
        aktør: Aktør,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        målform: Målform,
        personType: PersonType,
        enkelPersonInfo: Boolean = false,
        hentArbeidsforhold: Boolean = false,
    ): Person {
        val personinfo =
            if (enkelPersonInfo) {
                personopplysningerService.hentPersoninfoEnkel(aktør)
            } else {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
            }

        return Person(
            type = personType,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = personinfo.fødselsdato,
            aktør = aktør,
            navn = personinfo.navn ?: "",
            kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
            målform = målform,
        ).also { person ->
            person.opphold =
                personinfo.opphold?.map { GrOpphold.fraOpphold(it, person) }?.toMutableList() ?: mutableListOf()
            person.bostedsadresser =
                personinfo.bostedsadresser.filtrerUtKunNorskeBostedsadresser()
                    .map { GrBostedsadresse.fraBostedsadresse(it, person) }
                    .toMutableList()
            person.sivilstander = personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }.toMutableList()
            person.statsborgerskap =
                personinfo.statsborgerskap?.flatMap {
                    statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                        statsborgerskap = it,
                        person = person,
                    )
                }?.sortedBy { it.gyldigPeriode?.fom }?.toMutableList() ?: mutableListOf()
            person.dødsfall = lagDødsfallFraPdl(
                person = person,
                dødsfallDatoFraPdl = personinfo.dødsfall?.dødsdato,
                dødsfallAdresseFraPdl = personinfo.kontaktinformasjonForDoedsbo?.adresse,
            )
            if (person.hentSterkesteMedlemskap() == Medlemskap.EØS && hentArbeidsforhold) {
                person.arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(
                    person = person,
                ).toMutableList()
            }
        }
    }

    private fun hentFarEllerMedmorAktør(barna: List<Aktør>): Aktør? {
        val barnasFarEllerMedmorAktører =
            barna.map { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = it) }
                .flatMap { barn ->
                    barn.forelderBarnRelasjon.filter { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.FAR || it.relasjonsrolle == FORELDERBARNRELASJONROLLE.MEDMOR }
                }.map { it.aktør }.toSet()

        return barnasFarEllerMedmorAktører.singleOrNull()?.also {
            personidentService.hentOgLagreAktør(ident = it.aktørId, lagre = true)
        }
    }

    fun lagreOgDeaktiverGammel(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivPersongrunnlag = hentAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivPersongrunnlag != null) {
            personopplysningGrunnlagRepository.saveAndFlush(aktivPersongrunnlag.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    fun hentSøkersMålform(behandlingId: Long) =
        hentSøkerOgBarnPåBehandlingThrows(behandlingId).søker().målform

    @Transactional
    fun registrerManuellDødsfallPåPerson(
        behandlingId: BehandlingId,
        personIdent: PersonIdent,
        dødsfallDato: LocalDate,
        begrunnelse: String,
    ) {
        val personopplysningGrunnlag = hentAktivThrows(behandlingId.id)
        val aktør = personidentService.hentAktør(personIdent.ident)

        val person = personopplysningGrunnlag.personer.singleOrNull { it.aktør == aktør } ?: run {
            secureLogger.info("Klarte ikke registrere manuell dødsfall dato siden $aktør ikke finnes i personopplysningsgrunnlaget til behandlingen")
            throw Feil("Manuell registrering av dødsfall dato feilet i behandling ${behandlingId.id}. Se securelog for mer informasjon.")
        }

        validerAtDødsfallKanManueltRegistreresPåPerson(person, dødsfallDato)

        person.dødsfall = Dødsfall(person = person, dødsfallDato = dødsfallDato, manuellRegistrert = true)
        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId, dødsfallDato, aktør)
        loggService.loggManueltRegistrertDødsfallDato(behandlingId, person, begrunnelse)
    }

    private fun validerAtDødsfallKanManueltRegistreresPåPerson(person: Person, dødsfallDato: LocalDate) {
        when {
            person.erDød() -> throw FunksjonellFeil("Dødsfall dato er allerede registrert på person med navn ${person.navn}")
            person.fødselsdato > dødsfallDato -> throw FunksjonellFeil("Du kan ikke sette dødsfall dato til en dato som er før ${person.navn} sin fødselsdato")
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(PersongrunnlagService::class.java)
    }
}
