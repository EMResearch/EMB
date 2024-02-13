package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.YearMonth

class FagsakServiceTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,

    @Autowired
    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    /*
    This is a complicated test against following family relationship:
    søker3-----------
    (no case)       | (medmor)
                    barn2
                    | (medmor)
    søker1-----------
                    | (mor)
                    barn1
                    | (far)
    søker2-----------
                    | (far)
                    barn3

     We tests three search:
     1) search for søker1, one participant (søker1) should be returned
     2) search for barn1, three participants (barn1, søker1, søker2) should be returned
     3) search for barn2, three participants (barn2, søker3, søker1) should be returned, where fagsakId of søker3 is null
     */
    @Test
    fun `test å søke fagsak med fnr`() {
        val søker1Fnr = randomFnr()
        val søker2Fnr = randomFnr()
        val søker3Fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val søker1Aktør = personidentService.hentAktør(søker1Fnr)
        val søker2Aktør = personidentService.hentAktør(søker2Fnr)
        val søker3Aktør = personidentService.hentAktør(søker3Fnr)
        val barn1Aktør = personidentService.hentAktør(barn1Fnr)
        val barn2Aktør = personidentService.hentAktør(barn2Fnr)
        val barn3Aktør = personidentService.hentAktør(barn3Fnr)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(barn1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "barn1")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(barn2Aktør))
        } returns PersonInfo(
            fødselsdato = LocalDate.of(2019, 5, 1),
            kjønn = Kjønn.MANN,
            navn = "barn2",
            forelderBarnRelasjon = setOf(
                ForelderBarnRelasjon(
                    søker1Aktør,
                    FORELDERBARNRELASJONROLLE.MEDMOR,
                    "søker1",
                    LocalDate.of(1990, 2, 19),
                ),
                ForelderBarnRelasjon(
                    søker3Aktør,
                    FORELDERBARNRELASJONROLLE.MEDMOR,
                    "søker3",
                    LocalDate.of(1990, 1, 10),
                ),
            ),
        )

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(søker2Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1991, 2, 20), kjønn = Kjønn.MANN, navn = "søker2")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(barn2Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(2019, 5, 1), kjønn = Kjønn.MANN, navn = "barn2")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(barn3Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(2017, 3, 1), kjønn = Kjønn.KVINNE, navn = "barn3")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(søker2Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1991, 2, 20), kjønn = Kjønn.MANN, navn = "søker2")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(søker3Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1990, 1, 10), kjønn = Kjønn.KVINNE, navn = "søker3")

        val fagsak0 = fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                søker1Fnr,
            ),
        )

        val fagsak1 = fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                søker2Fnr,
            ),
        )

        val førsteBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker1Fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak0.data!!.id,
            ),
        )
        stegService.håndterPersongrunnlag(
            førsteBehandling,
            RegistrerPersongrunnlagDTO(ident = søker1Fnr, barnasIdenter = listOf(barn1Fnr)),
        )

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.AVSLUTTET)

        val andreBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker1Fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak0.data!!.id,
            ),
        )
        stegService.håndterPersongrunnlag(
            andreBehandling,
            RegistrerPersongrunnlagDTO(
                ident = søker1Fnr,
                barnasIdenter = listOf(barn1Fnr, barn2Fnr),
            ),
        )

        val tredjeBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker2Fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak1.data!!.id,
            ),
        )
        stegService.håndterPersongrunnlag(
            tredjeBehandling,
            RegistrerPersongrunnlagDTO(ident = søker2Fnr, barnasIdenter = listOf(barn1Fnr)),
        )

        val søkeresultat1 = fagsakService.hentFagsakDeltager(søker1Fnr)
        assertEquals(1, søkeresultat1.size)
        assertEquals(Kjønn.KVINNE, søkeresultat1[0].kjønn)
        assertEquals("søker1", søkeresultat1[0].navn)
        assertEquals(fagsak0.data!!.id, søkeresultat1[0].fagsakId)

        val søkeresultat2 = fagsakService.hentFagsakDeltager(barn1Fnr)
        assertEquals(3, søkeresultat2.size)
        var matching = 0
        søkeresultat2.forEach {
            matching += if (it.fagsakId == fagsak0.data!!.id) 1 else if (it.fagsakId == fagsak1.data!!.id) 10 else 0
        }
        assertEquals(11, matching)
        assertEquals(1, søkeresultat2.filter { it.ident == barn1Fnr }.size)

        val søkeresultat3 = fagsakService.hentFagsakDeltager(barn2Fnr)
        assertEquals(3, søkeresultat3.size)
        assertEquals(1, søkeresultat3.filter { it.ident == barn2Fnr }.size)
        assertNull(søkeresultat3.find { it.ident == barn2Fnr }!!.fagsakId)
        assertEquals(fagsak0.data!!.id, søkeresultat3.find { it.ident == søker1Fnr }!!.fagsakId)
        assertEquals(1, søkeresultat3.filter { it.ident == søker3Fnr }.size)
        assertEquals("søker3", søkeresultat3.filter { it.ident == søker3Fnr }[0].navn)
        assertNull(søkeresultat3.find { it.ident == søker3Fnr }!!.fagsakId)

        val fagsak = fagsakService.hentNormalFagsak(søker1Aktør)!!

        assertEquals(
            FagsakStatus.OPPRETTET.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SAK, fagsak.id)
                .last().jsonToSakDVH().sakStatus,
        )
    }

    @Test
    fun `Skal teste at arkiverte fagsaker med behandling ikke blir funnet ved søk`() {
        val søker1Fnr = randomFnr()
        val søker1Aktør = tilAktør(søker1Fnr)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1991, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1991, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                søker1Fnr,
            ),
        )

        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker1Fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.data!!.id,
            ),
        )

        fagsakService.lagre(
            fagsakService.hentFagsakPåPerson(søker1Aktør).also { it?.arkivert = true }!!,
        )

        val søkeresultat1 = fagsakService.hentFagsakDeltager(søker1Fnr)

        assertEquals(1, søkeresultat1.size)
        assertNull(søkeresultat1.first().fagsakId)
    }

    @Test
    fun `Skal teste at arkiverte fagsaker uten behandling ikke blir funnet ved søk`() {
        val søker1Fnr = randomFnr()
        val søker1Aktør = tilAktør(søker1Fnr)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1992, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(eq(søker1Aktør))
        } returns PersonInfo(fødselsdato = LocalDate.of(1992, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                søker1Fnr,
            ),
        )

        fagsakService.lagre(
            fagsakService.hentFagsakPåPerson(søker1Aktør).also { it?.arkivert = true }!!,
        )

        val søkeresultat1 = fagsakService.hentFagsakDeltager(søker1Fnr)

        assertEquals(1, søkeresultat1.size)
        assertNull(søkeresultat1.first().fagsakId)
    }

    @Test
    fun `Skal teste at man henter alle fagsakene til barnet`() {
        val barnFnr = randomFnr()

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        fun opprettGrunnlag(behandling: Behandling) = lagTestPersonopplysningGrunnlag(
            behandling.id,
            behandling.fagsak.aktør.aktivFødselsnummer(),
            listOf(barnFnr),
            søkerAktør = behandling.fagsak.aktør,
            barnAktør = barnAktør,
        )

        val fagsakMor = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingMor = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsakMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor))
        behandlingService.oppdaterStatusPåBehandling(behandlingMor.id, BehandlingStatus.AVSLUTTET)
        val behandlingMor2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsakMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor2))

        val fagsakFar = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingFar = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsakFar))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingFar))

        val fagsaker = fagsakService.hentFagsakerPåPerson(barnAktør.first())
        assertEquals(2, fagsaker.size)
        assertThat(persongrunnlagRepository.findAll()).hasSize(4)
    }

    // Satte XX for at dette testet skal kjøre sist.
    @Test
    fun `XX Søk på fnr som ikke finnes i PDL skal vi tom liste`() {
        every {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
        } answers {
            throw HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "[PdlRestClient][Feil ved oppslag på person: Fant ikke person]",
            )
        }
        assertEquals(emptyList<RestFagsakDeltager>(), fagsakService.hentFagsakDeltager(randomFnr()))
    }

    @Test
    fun `Skal kun hente løpende fagsak for søker`() {
        val søker = lagPerson(type = PersonType.SØKER)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = søker.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(søker.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente løpende institusjonsfagsak for søker`() {
        val barn = lagPerson(type = PersonType.BARN)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = barn.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE, fagsakType = FagsakType.INSTITUSJON)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente fagsak hvor barn har løpende andel`() {
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsak = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = barn.aktør,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = barn.aktør,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsak, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsak, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal ikke hente fagsak hvor barn har andel som ikke er løpende`() {
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsak = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = barn.aktør,
            ),
        )
        opprettAndelerOgBehandling(fagsak = fagsak, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal hente to fagsaker hvis aktør er søker i en sak og blir mottatt barnetrygd for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedSøkerSomDeltaker.first())
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.last())
    }

    @Test
    fun `Skal ikke hente fagsak hvis barn kun har løpende andeler i en gammel behandling som senere er opphørt`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val gamlePerioder = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
            ),
        )

        val nyePerioder = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = gamlePerioder) // gammel behandling
        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = nyePerioder) // ny behandling

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal returnere fagsak hvor person mottar løpende utvidet`() {
        val person = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)

        val perioder = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().plusMonths(6),
                aktør = barn.aktør,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioder)

        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(1, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedPersonSomFårUtvidetEllerOrdinær.single())
    }

    @Test
    fun `Skal returnere ikke fagsak hvor person mottok utvidet som ikke er løpende lenger`() {
        val person = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)

        val perioder = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().plusMonths(6),
                aktør = barn.aktør,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioder)

        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(0, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
    }

    @Test
    fun `Skal kun hente én fagsak hvis aktør er søker i en sak (uten løpende utvidet) og blir mottatt barnetrygd for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE) // Fagsak hvor person er søker, men ikke har noen løpende utvidet-andeler
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente to fagsaker hvor person mottar løpende utvidet i en behandling og blir mottatt løpende ordinær for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilFagsakBarn = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
            ),
        )

        val perioderTilFagsakSøker = listOf(
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().minusMonths(3),
                aktør = person.aktør,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            ),
            PeriodeForAktør(
                fom = YearMonth.now().minusMonths(2),
                tom = YearMonth.now().plusMonths(6),
                aktør = person.aktør,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            ),
        )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilFagsakBarn)
        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilFagsakSøker)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.first())
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedSøkerSomDeltaker.last())
    }

    private data class PeriodeForAktør(
        val fom: YearMonth,
        val tom: YearMonth,
        val aktør: Aktør,
        val ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    )

    private fun opprettFagsakForPersonMedStatus(personIdent: String, fagsakStatus: FagsakStatus, fagsakType: FagsakType = FagsakType.NORMAL): Fagsak {
        val institusjon = InstitusjonInfo(orgNummer = "123456789", tssEksternId = "testid")
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fødselsnummer = personIdent, fagsakType = fagsakType, institusjon = if (fagsakType == FagsakType.INSTITUSJON) institusjon else null)
        return fagsakService.oppdaterStatus(fagsak, fagsakStatus)
    }

    private fun opprettAndelerOgBehandling(fagsak: Fagsak, barnasIdenter: List<String>, perioderTilAndeler: List<PeriodeForAktør>) {
        val nyBehandling = NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            søkersIdent = fagsak.aktør.aktivFødselsnummer(),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            navIdent = randomFnr(),
            barnasIdenter = barnasIdenter,
            søknadMottattDato = LocalDate.now().minusMonths(1),
            fagsakId = fagsak.id,
        )
        val behandling = behandlingService.opprettBehandling(nyBehandling = nyBehandling)
        val tilkjentYtelse = TilkjentYtelse(behandling = behandling, endretDato = LocalDate.now(), opprettetDato = LocalDate.now())
        val andelerTilkjentYtelse = perioderTilAndeler.map {
            lagAndelTilkjentYtelse(
                fom = it.fom,
                tom = it.tom,
                aktør = it.aktør,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                ytelseType = it.ytelseType,
            )
        }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        tilkjentYtelseRepository.save(tilkjentYtelse)

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)
    }
}
