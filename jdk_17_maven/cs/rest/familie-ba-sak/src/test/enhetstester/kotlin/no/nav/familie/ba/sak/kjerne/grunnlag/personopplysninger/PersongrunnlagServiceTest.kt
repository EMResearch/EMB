package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class PersongrunnlagServiceTest {
    val personidentService = mockk<PersonidentService>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val personopplysningerService = mockk<PersonopplysningerService>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val loggService = mockk<LoggService>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()

    val persongrunnlagService = spyk(
        PersongrunnlagService(
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            statsborgerskapService = mockk(),
            arbeidsfordelingService = mockk(relaxed = true),
            personopplysningerService = personopplysningerService,
            personidentService = personidentService,
            saksstatistikkEventPublisher = mockk(relaxed = true),
            behandlingHentOgPersisterService = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            arbeidsforholdService = mockk(),
            vilkårsvurderingService = vilkårsvurderingService,
        ),
    )

    @Test
    fun `Skal sende med barna fra forrige behandling ved førstegangsbehandling nummer to`() {
        val søker = lagPerson()
        val barnFraForrigeBehandling = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)

        val barnFnr = barn.aktør.aktivFødselsnummer()
        val søkerFnr = søker.aktør.aktivFødselsnummer()

        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val forrigeBehandlingPersongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = forrigeBehandling.id,
                personer = arrayOf(søker, barnFraForrigeBehandling),
            )

        val søknadDTO = lagSøknadDTO(
            søkerIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
        )

        every { personidentService.hentOgLagreAktør(søkerFnr, true) } returns søker.aktør
        every { personidentService.hentOgLagreAktør(barnFnr, true) } returns barn.aktør

        every { persongrunnlagService.hentAktiv(forrigeBehandling.id) } returns forrigeBehandlingPersongrunnlag

        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                forrigeBehandling.id,
                barnFraForrigeBehandling.aktør,
            )
        } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now(), tom = YearMonth.now()))

        every {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns PersonopplysningGrunnlag(behandlingId = behandling.id)

        persongrunnlagService.registrerBarnFraSøknad(
            søknadDTO = søknadDTO,
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )
        verify(exactly = 1) {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker.aktør,
                barnFraInneværendeBehandling = listOf(barn.aktør),
                barnFraForrigeBehandling = listOf(barnFraForrigeBehandling.aktør),
                behandling = behandling,
                målform = søknadDTO.søkerMedOpplysninger.målform,
            )
        }
    }

    @Test
    fun `hentOgLagreSøkerOgBarnINyttGrunnlag skal på inst- og EM-saker kun lagre èn instans av barnet, med personType BARN`() {
        val barnet = lagPerson()
        val behandlinger = listOf(FagsakType.INSTITUSJON, FagsakType.BARN_ENSLIG_MINDREÅRIG).map { fagsakType ->
            lagBehandling(fagsak = defaultFagsak().copy(type = fagsakType))
        }
        behandlinger.forEach { behandling ->
            val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

            every {
                persongrunnlagService.lagreOgDeaktiverGammel(any())
            } returns nyttGrunnlag

            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnet.aktør)
            } returns PersonInfo(barnet.fødselsdato, barnet.navn, barnet.kjønn)

            every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = barnet.aktør,
                barnFraInneværendeBehandling = listOf(barnet.aktør),
                barnFraForrigeBehandling = listOf(barnet.aktør),
                behandling = behandling,
                målform = Målform.NB,
            ).apply {
                Assertions.assertThat(this.personer)
                    .hasSize(1)
                    .extracting("type")
                    .containsExactly(PersonType.BARN)
            }
        }
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal kaste feil dersom man registrer dødsfall dato før personen er født`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person = lagPerson(fødselsdato = dødsfallsDato.plusMonths(10))
        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            persongrunnlagService.registrerManuellDødsfallPåPerson(
                behandlingId = BehandlingId(behandling.id),
                personIdent = PersonIdent(personFnr),
                dødsfallDato = dødsfallsDato,
                begrunnelse = "test",
            )
        }

        assertThat(funksjonellFeil.melding, Is("Du kan ikke sette dødsfall dato til en dato som er før SØKER sin fødselsdato"))
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal kaste feil dersom man registrer dødsfall dato når personen allerede har dødsfallsdato registrert`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person = lagPerson(fødselsdato = dødsfallsDato.minusMonths(10)).also {
            it.dødsfall = Dødsfall(person = it, dødsfallDato = dødsfallsDato)
        }

        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            persongrunnlagService.registrerManuellDødsfallPåPerson(
                behandlingId = BehandlingId(behandling.id),
                personIdent = PersonIdent(personFnr),
                dødsfallDato = dødsfallsDato,
                begrunnelse = "test",
            )
        }

        assertThat(funksjonellFeil.melding, Is("Dødsfall dato er allerede registrert på person med navn ${person.navn}"))
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal endre på vilkår og logge at manuelt dødsfalldato er registrert`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person = lagPerson(fødselsdato = dødsfallsDato.minusMonths(10))

        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør
        every { loggService.loggManueltRegistrertDødsfallDato(any(), any(), "test") } returns mockk()
        every { vilkårsvurderingService.oppdaterVilkårVedDødsfall(any(), any(), any()) } just runs

        persongrunnlagService.registrerManuellDødsfallPåPerson(
            behandlingId = BehandlingId(behandling.id),
            personIdent = PersonIdent(personFnr),
            dødsfallDato = dødsfallsDato,
            begrunnelse = "test",
        )

        verify(exactly = 1) { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) }
        verify(exactly = 1) { personidentService.hentAktør(personFnr) }
        verify(exactly = 1) { loggService.loggManueltRegistrertDødsfallDato(any(), any(), "test") }
        verify(exactly = 1) { vilkårsvurderingService.oppdaterVilkårVedDødsfall(any(), any(), any()) }
    }
}
