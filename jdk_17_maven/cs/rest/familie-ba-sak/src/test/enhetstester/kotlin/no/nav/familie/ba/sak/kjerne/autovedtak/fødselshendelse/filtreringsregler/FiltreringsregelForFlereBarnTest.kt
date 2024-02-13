package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfallFraPdl
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class FiltreringsregelForFlereBarnTest {

    val barnAktør0 = randomAktør()
    val barnAktør1 = randomAktør()
    val gyldigAktør = randomAktør()

    val personopplysningGrunnlagRepositoryMock = mockk<PersonopplysningGrunnlagRepository>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val personidentService = mockk<PersonidentService>()
    val localDateServiceMock = mockk<LocalDateService>()
    val fødselshendelsefiltreringResultatRepository = mockk<FødselshendelsefiltreringResultatRepository>(relaxed = true)
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val tilkjentYtelseValideringServiceMock = mockk<TilkjentYtelseValideringService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val filtreringsreglerService = FiltreringsreglerService(
        personopplysningerService = personopplysningerServiceMock,
        personidentService = personidentService,
        personopplysningGrunnlagRepository = personopplysningGrunnlagRepositoryMock,
        localDateService = localDateServiceMock,
        fødselshendelsefiltreringResultatRepository = fødselshendelsefiltreringResultatRepository,
        behandlingService = behandlingServiceMock,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        tilkjentYtelseValideringService = tilkjentYtelseValideringServiceMock,
        vilkårsvurderingRepository = vilkårsvurderingRepository,
        andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
    )

    init {
        val fødselshendelsefiltreringResultatSlot = slot<List<FødselshendelsefiltreringResultat>>()
        every { fødselshendelsefiltreringResultatRepository.saveAll(capture(fødselshendelsefiltreringResultatSlot)) } answers {
            fødselshendelsefiltreringResultatSlot.captured
        }
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            genererFaktaMedTidligereBarn(1, 3, 7, 0),
        )

        Assertions.assertThat(evalueringer.erOppfylt()).isFalse
        Assertions.assertThat(
            evalueringer
                .filter { it.resultat == Resultat.IKKE_OPPFYLT }
                .any { it.identifikator == Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.name },
        )
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har ikke gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            genererFaktaMedTidligereBarn(0, 0, 0, 5),
        )

        Assertions.assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på minst ett barn`() {
        val behandling = lagBehandling()
        val personInfo = generePersonInfoMedBarn(setOf(barnAktør0, barnAktør1))

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, aktiv = true).apply {
            personer.addAll(
                listOf(
                    genererPerson(
                        type = PersonType.SØKER,
                        personopplysningGrunnlag = this,
                        aktør = gyldigAktør,
                    ),
                    genererPerson(
                        type = PersonType.BARN,
                        personopplysningGrunnlag = this,
                        aktør = barnAktør0,
                        fødselsDato = LocalDate.now().minusMonths(1),
                        dødsfallDato = LocalDate.now().toString(),
                    ),
                    genererPerson(
                        type = PersonType.BARN,
                        personopplysningGrunnlag = this,
                        aktør = barnAktør1,
                        fødselsDato = LocalDate.now().minusMonths(1),
                    ),
                ),
            )
        }

        every { personopplysningGrunnlagRepositoryMock.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag

        every { personopplysningerServiceMock.hentPersoninfoMedRelasjonerOgRegisterinformasjon(gyldigAktør) } returns personInfo

        every { personopplysningerServiceMock.harVerge(gyldigAktør) } returns VergeResponse(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(15)

        every { personidentService.hentAktør(gyldigAktør.aktivFødselsnummer()) } returns gyldigAktør

        val andelTilkjentytelse = listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.now().plusYears(1)),
        )
            .map {
                lagAndelTilkjentYtelse(it.fom, it.tom)
            }
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andelTilkjentytelse

        every {
            personidentService.hentAktørIder(
                listOf(
                    barnAktør0.aktivFødselsnummer(),
                    barnAktør1.aktivFødselsnummer(),
                ),
            )
        } returns listOf(barnAktør0, barnAktør1)

        every { tilkjentYtelseValideringServiceMock.barnetrygdLøperForAnnenForelder(any(), any()) } returns false

        val sisteVedtatteBehandling = lagBehandling()
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns sisteVedtatteBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            gyldigAktør.tilPerson(personopplysningGrunnlag)!!,
            listOf(barnAktør0.tilPerson(personopplysningGrunnlag)!!, barnAktør1.tilPerson(personopplysningGrunnlag)!!),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    gyldigAktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 2, 1),
                        ),
                    ),
                ),
            ),
        )

        val fødselshendelsefiltreringResultater = filtreringsreglerService.kjørFiltreringsregler(
            NyBehandlingHendelse(
                morsIdent = gyldigAktør.aktivFødselsnummer(),
                barnasIdenter = listOf(
                    barnAktør0.aktivFødselsnummer(),
                    barnAktør1.aktivFødselsnummer(),
                ),
            ),
            behandling,
        )

        Assertions.assertThat(fødselshendelsefiltreringResultater.erOppfylt()).isFalse
        Assertions.assertThat(
            fødselshendelsefiltreringResultater
                .filter { it.resultat == Resultat.IKKE_OPPFYLT }
                .any { it.filtreringsregel == Filtreringsregel.BARN_LEVER },
        )
    }

    @Test
    fun `Regelevaluering skal resultere i JA når alle filtreringsregler er oppfylt`() {
        val behandling = lagBehandling()
        val personInfo = generePersonInfoMedBarn(setOf(barnAktør0, barnAktør1))

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, aktiv = true).apply {
            personer.addAll(
                listOf(
                    genererPerson(
                        type = PersonType.SØKER,
                        personopplysningGrunnlag = this,
                        aktør = gyldigAktør,
                    ),
                    genererPerson(
                        type = PersonType.BARN,
                        personopplysningGrunnlag = this,
                        aktør = barnAktør0,
                        fødselsDato = LocalDate.now().minusMonths(1),
                    ),
                    genererPerson(
                        type = PersonType.BARN,
                        personopplysningGrunnlag = this,
                        aktør = barnAktør1,
                        fødselsDato = LocalDate.now().minusMonths(1),
                    ),
                ),
            )
        }

        every { personopplysningGrunnlagRepositoryMock.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag

        every { personopplysningerServiceMock.hentPersoninfoMedRelasjonerOgRegisterinformasjon(gyldigAktør) } returns personInfo

        every { personopplysningerServiceMock.harVerge(gyldigAktør) } returns VergeResponse(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(20)

        every { personidentService.hentAktør(gyldigAktør.aktivFødselsnummer()) } returns gyldigAktør
        every {
            personidentService.hentAktørIder(
                listOf(
                    barnAktør0.aktivFødselsnummer(),
                    barnAktør1.aktivFødselsnummer(),
                ),
            )
        } returns listOf(barnAktør0, barnAktør1)

        every { tilkjentYtelseValideringServiceMock.barnetrygdLøperForAnnenForelder(any(), any()) } returns false

        val andelTilkjentytelse = listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.now().plusYears(1)),
        )
            .map {
                lagAndelTilkjentYtelse(it.fom, it.tom)
            }
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andelTilkjentytelse

        val sisteVedtatteBehandling = lagBehandling()
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns sisteVedtatteBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            gyldigAktør.tilPerson(personopplysningGrunnlag)!!,
            listOf(barnAktør0.tilPerson(personopplysningGrunnlag)!!, barnAktør1.tilPerson(personopplysningGrunnlag)!!),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    gyldigAktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 2, 1),
                        ),
                    ),
                ),
            ),
        )

        val fødselshendelsefiltreringResultater = filtreringsreglerService.kjørFiltreringsregler(
            NyBehandlingHendelse(
                morsIdent = gyldigAktør.aktivFødselsnummer(),
                barnasIdenter = listOf(
                    barnAktør0.aktivFødselsnummer(),
                    barnAktør1.aktivFødselsnummer(),
                ),
            ),
            behandling,
        )

        Assertions.assertThat(fødselshendelsefiltreringResultater.erOppfylt()).isTrue
    }

    private fun genererPerson(
        type: PersonType,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        aktør: Aktør,
        fødselsDato: LocalDate? = null,
        grBostedsadresse: GrBostedsadresse? = null,
        kjønn: Kjønn = Kjønn.KVINNE,
        sivilstand: SIVILSTAND = SIVILSTAND.UGIFT,
        dødsfallDato: String? = null,
    ): Person {
        return Person(
            aktør = aktør,
            type = type,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = fødselsDato ?: LocalDate.of(1991, 1, 1),
            navn = "navn",
            kjønn = kjønn,
            bostedsadresser = grBostedsadresse?.let { mutableListOf(grBostedsadresse) } ?: mutableListOf(),
        )
            .apply {
                this.sivilstander = mutableListOf(GrSivilstand(type = sivilstand, person = this))
                if (dødsfallDato != null) {
                    this.dødsfall = lagDødsfallFraPdl(
                        person = this,
                        dødsfallDatoFraPdl = dødsfallDato,
                        dødsfallAdresseFraPdl = PdlKontaktinformasjonForDødsboAdresse(
                            adresselinje1 = "Gate 1",
                            postnummer = "1234",
                            poststedsnavn = "Oslo",
                        ),
                    )
                }
            }
    }

    private fun generePersonInfoMedBarn(
        barn: Set<Aktør>? = null,
        navn: String = "Noname",
        fødselsDato: LocalDate? = null,
        adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
        bostedsadresse: Bostedsadresse? = null,
        sivilstand: SIVILSTAND = SIVILSTAND.UGIFT,
    ): PersonInfo {
        return PersonInfo(
            fødselsdato = fødselsDato ?: LocalDate.now().minusYears(20),
            navn = navn,
            adressebeskyttelseGradering = adressebeskyttelsegradering,
            bostedsadresser = bostedsadresse?.let { mutableListOf(it) } ?: mutableListOf(Bostedsadresse()),
            sivilstander = listOf(Sivilstand(type = sivilstand)),
            forelderBarnRelasjon = barn?.map {
                ForelderBarnRelasjon(
                    aktør = it,
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    navn = "navn $it",
                )
            }?.toSet() ?: emptySet(),
        )
    }

    private fun genererFaktaMedTidligereBarn(
        manaderFodselEtt: Long,
        manaderFodselTo: Long,
        manaderFodselForrigeFodsel: Long,
        dagerFodselForrigeFodsel: Long,
    ): FiltreringsreglerFakta {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktør)
        val barn = listOf(
            tilfeldigPerson(LocalDate.now().minusMonths(manaderFodselEtt)).copy(aktør = barnAktør0),
            tilfeldigPerson(LocalDate.now().minusMonths(manaderFodselTo)).copy(aktør = barnAktør1),
        )

        val restenAvBarna: List<PersonInfo> = listOf(
            PersonInfo(LocalDate.now().minusMonths(manaderFodselForrigeFodsel).minusDays(dagerFodselForrigeFodsel)),
        )

        return FiltreringsreglerFakta(
            mor = mor,
            barnaFraHendelse = barn,
            restenAvBarna = restenAvBarna,
            morLever = true,
            barnaLever = true,
            morHarVerge = false,
            dagensDato = LocalDate.now(),
            erFagsakenMigrertEtterBarnFødt = false,
            løperBarnetrygdForBarnetPåAnnenForelder = false,
            morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
            morHarIkkeOpphørtBarnetrygd = true,
        )
    }
}
