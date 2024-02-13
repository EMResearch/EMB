package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringSvar
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring.Companion.SATSENDRINGMÅNED_MARS_2023
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class BehandlingSatsendringTest(
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val andelTilkjentYtelseMedEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {

    @BeforeEach
    fun setUp() {
        mockkObject(SatsTidspunkt)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2023, 2, 1)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal kjøre satsendring på løpende fagsak hvor brukeren har barnetrygd under 6 år`() {
        val scenario = mockServerKlient().lagScenario(restScenario)
        val behandling = opprettBehandling(scenario)
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id, satsTidspunkt = SATSENDRINGMÅNED_MARS_2023))

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val satsendringResultat =
            autovedtakSatsendringService.kjørBehandling(SatsendringTaskDto(behandling.fagsak.id, YearMonth.of(2023, 3)))

        assertEquals(SatsendringSvar.SATSENDRING_KJØRT_OK, satsendringResultat)

        val satsendringBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, satsendringBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendringBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendringBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)

        val aty = andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
            satsendringBehandling.id,
        )

        val atyMedSenesteTilleggOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2023, 7) }
        val atyMedVanligOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2029, 1) }
        assertThat(atyMedSenesteTilleggOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp)
        assertThat(atyMedVanligOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.ORBA).beløp)

        val satskjøring = satskjøringRepository.findByFagsakIdAndSatsTidspunkt(behandling.fagsak.id, satsTidspunkt = SATSENDRINGMÅNED_MARS_2023)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))
    }

    @Test
    fun `Skal ignorere satsendring hvis siste sats alt er satt`() {
        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val scenario = mockServerKlient().lagScenario(restScenario)
        val behandling = opprettBehandling(scenario)
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id, satsTidspunkt = SATSENDRINGMÅNED_MARS_2023))

        val satsendringResultat =
            autovedtakSatsendringService.kjørBehandling(SatsendringTaskDto(behandling.fagsak.id, YearMonth.of(2023, 3)))

        assertEquals(SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT, satsendringResultat)

        val satskjøring = satskjøringRepository.findByFagsakIdAndSatsTidspunkt(behandling.fagsak.id, satsTidspunkt = SATSENDRINGMÅNED_MARS_2023)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))
    }

    private val matrikkeladresse = Matrikkeladresse(
        matrikkelId = 123L,
        bruksenhetsnummer = "H301",
        tilleggsnavn = "navn",
        postnummer = "0202",
        kommunenummer = "2231",
    )
    private val restScenario = RestScenario(
        søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    angittFlyttedato = LocalDate.now().minusYears(10),
                    gyldigTilOgMed = null,
                    matrikkeladresse = matrikkeladresse,
                ),
            ),
        ),
        barna = listOf(
            RestScenarioPerson(
                fødselsdato = LocalDate.of(2023, 1, 1).toString(),
                fornavn = "Barn",
                etternavn = "Barnesen",
            ).copy(
                bostedsadresser = mutableListOf(
                    Bostedsadresse(
                        angittFlyttedato = LocalDate.now().minusYears(6),
                        gyldigTilOgMed = null,
                        matrikkeladresse = matrikkeladresse,
                    ),
                ),
            ),
        ),
    )

    private fun opprettBehandling(scenario: RestScenario) =
        behandleFødselshendelse(
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
}
