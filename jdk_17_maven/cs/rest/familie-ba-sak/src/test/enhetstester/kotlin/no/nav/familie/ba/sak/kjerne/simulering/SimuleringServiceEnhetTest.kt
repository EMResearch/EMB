package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGeneratorService
import no.nav.familie.ba.sak.integrasjoner.økonomi.lagUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.tilRestUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.unleash.UnleashService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class SimuleringServiceEnhetTest {

    private val økonomiKlient: ØkonomiKlient = mockk()
    private val økonomiService: ØkonomiService = mockk()
    private val beregningService: BeregningService = mockk()
    private val økonomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository = mockk()
    private val tilgangService: TilgangService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val vedtakRepository: VedtakRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService = mockk()
    private val utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService = mockk()
    private val unleashService: UnleashService = mockk()

    private val simuleringService: SimuleringService = SimuleringService(
        økonomiKlient,
        beregningService,
        økonomiSimuleringMottakerRepository,
        tilgangService,
        featureToggleService,
        unleashService,
        vedtakRepository,
        utbetalingsoppdragGeneratorService,
        behandlingHentOgPersisterService,
        persongrunnlagService,
        kontrollerNyUtbetalingsgeneratorService,
    )

    val februar2023 = LocalDate.of(2023, 2, 1)

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av etterbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )

        // etterbetaling 4 KR pga. avrundingsfeil. 1 KR per barn i hver periode.
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = -2, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -2, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT),
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns listOf(
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av feilutbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(4)

        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        val fom2 = LocalDate.of(2021, 2, 1)
        val tom2 = LocalDate.of(2021, 2, 28)

        // feilutbetaling 1 KR per barn i hver periode
        val posteringer = listOf(
            mockVedtakSimuleringPostering(
                fom = fom,
                tom = tom,
                beløp = 2,
                posteringType = PosteringType.FEILUTBETALING,
            ),
            mockVedtakSimuleringPostering(
                fom = fom2,
                tom = tom2,
                beløp = 2,
                posteringType = PosteringType.FEILUTBETALING,
            ),
        )

        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns listOf(
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere false dersom det finnes avvik i form av feilutbetaling som er utenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns listOf(
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            lagPerson(type = PersonType.BARN).tilPersonEnkel(),
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(false))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal kaste feil dersom behandlingen ikke er en manuell migrering`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )

        assertThrows<Feil> { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal returnere true dersom det finnes manuelle posteringer i simuleringsresultat før mars 2023`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(
                beløp = 200,
                betalingType = BetalingType.DEBIT,
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
            ),
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker

        val behandlingHarManuellePosteringerFørMars2023 =
            simuleringService.harMigreringsbehandlingManuellePosteringer(behandling)

        assertThat(behandlingHarManuellePosteringerFørMars2023, Is(true))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal returnere false dersom det ikke finnes manuelle posteringer i simuleringsresultat før mars 2023`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker

        val behandlingHarManuellePosteringerFørMars2023 =
            simuleringService.harMigreringsbehandlingManuellePosteringer(behandling)

        assertThat(behandlingHarManuellePosteringerFørMars2023, Is(false))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal kaste feil dersom behandlingen ikke er en manuell migrering`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )

        assertThrows<Feil> { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) }
    }

    @Test
    fun `hentSimuleringFraFamilieOppdrag - skal bruke gammel utbetalingsgenerator når toggel er av`() {
        setupMocksForFeatureToggleTests(false)
        simuleringService.hentSimuleringFraFamilieOppdrag(lagVedtak())

        verify(exactly = 1) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
                erSimulering = any(),
            )
        }

        verify(exactly = 0) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        }
    }

    @Test
    fun `hentSimuleringFraFamilieOppdrag - skal bruke ny utbetalingsgenerator når toggel er på`() {
        setupMocksForFeatureToggleTests(true)
        simuleringService.hentSimuleringFraFamilieOppdrag(lagVedtak())

        verify(exactly = 0) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
                erSimulering = any(),
            )
        }

        verify(exactly = 1) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        }
    }

    private fun setupMocksForFeatureToggleTests(togglePå: Boolean) {
        every { beregningService.erEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(any()) } returns true

        every {
            unleashService.isEnabled(
                toggleId = any(),
                properties = any(),
            )
        } returns togglePå

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(
            listOf(
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    opphør = null,
                    periodeId = 1,
                    forrigePeriodeId = null,
                    datoForVedtak = LocalDate.now(),
                    klassifisering = "BATR",
                    vedtakdatoFom = inneværendeMåned().førsteDagIInneværendeMåned(),
                    vedtakdatoTom = inneværendeMåned().sisteDagIInneværendeMåned(),
                    sats = BigDecimal(1054),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = "13455678910",
                    behandlingId = 1,
                ),
            ),
        )

        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = any(),
                erSimulering = any(),
            )
        } returns utbetalingsoppdrag.tilRestUtbetalingsoppdrag()

        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        } returns BeregnetUtbetalingsoppdragLongId(utbetalingsoppdrag = utbetalingsoppdrag, andeler = emptyList())

        every { økonomiKlient.hentSimulering(utbetalingsoppdrag.tilRestUtbetalingsoppdrag()) } returns mockk()

        every {
            kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
                vedtak = any(),
                gammeltSimuleringResultat = any(),
                gammeltUtbetalingsoppdrag = any(),
                erSimulering = any(),
            )
        } returns mockk()
    }

    private fun mockØkonomiSimuleringMottaker(
        id: Long = 0,
        mottakerNummer: String? = randomFnr(),
        mottakerType: MottakerType = MottakerType.BRUKER,
        behandling: Behandling = mockk(relaxed = true),
        økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockVedtakSimuleringPostering()),
    ) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)

    private fun mockVedtakSimuleringPostering(
        økonomiSimuleringMottaker: ØkonomiSimuleringMottaker = mockk(relaxed = true),
        beløp: Int = 0,
        fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.BARNETRYGD,
        fom: LocalDate = LocalDate.of(2023, 1, 1),
        tom: LocalDate = LocalDate.of(2023, 1, 1),
        betalingType: BetalingType = BetalingType.DEBIT,
        posteringType: PosteringType = PosteringType.YTELSE,
        forfallsdato: LocalDate = LocalDate.of(2023, 1, 1),
        utenInntrekk: Boolean = false,
    ) = ØkonomiSimuleringPostering(
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
        fagOmrådeKode = fagOmrådeKode,
        fom = fom,
        tom = tom,
        betalingType = betalingType,
        beløp = beløp.toBigDecimal(),
        posteringType = posteringType,
        forfallsdato = forfallsdato,
        utenInntrekk = utenInntrekk,
    )
}
