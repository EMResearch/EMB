package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.filtrerLikEllerEtterEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeServiceIntegrationTest(
    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val brevmalService: BrevmalService,

) : AbstractSpringIntegrationTest() {

    val søkerFnr = randomFnr()
    val barnFnr = ClientMocks.barnFnr[0]
    val barn2Fnr = ClientMocks.barnFnr[1]

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    private fun kjørFørstegangsbehandlingOgRevurderingÅrligKontroll(): Behandling {
        val førstegangsbehandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr, barn2Fnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        return kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr, barn2Fnr),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = førstegangsbehandling.fagsak.id,
            brevmalService = brevmalService,
        )
    }

    @Test
    fun `Skal lage og populere avslagsperiode for uregistrert barn`() {
        val søkerFnr = randomFnr()
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.REGISTRERE_SØKNAD,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val behandlingEtterNySøknadsregistrering = stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = SøknadDTO(
                    underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                    søkerMedOpplysninger = SøkerMedOpplysninger(
                        ident = søkerFnr,
                    ),
                    barnaMedOpplysninger = listOf(
                        BarnMedOpplysninger(
                            ident = "",
                            erFolkeregistrert = false,
                            inkludertISøknaden = true,
                        ),
                    ),
                    endringAvOpplysningerBegrunnelse = "",
                ),
                bekreftEndringerViaFrontend = true,
            ),
        )

        val vedtaksperioder =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingEtterNySøknadsregistrering.id)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(1, vedtaksperioder.flatMap { it.begrunnelser }.size)
        assertEquals(
            Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN,
            vedtaksperioder.flatMap { it.begrunnelser }.first().standardbegrunnelse,
        )
    }

    @Test
    fun `Skal lage og populere avslagsperiode for uregistrert barn med eøs begrunnelse dersom behandling sin kategori er EØS`() {
        val søkerFnr = randomFnr()
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.REGISTRERE_SØKNAD,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            behandlingKategori = BehandlingKategori.EØS,
        )

        val behandlingEtterNySøknadsregistrering = stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = SøknadDTO(
                    underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                    søkerMedOpplysninger = SøkerMedOpplysninger(
                        ident = søkerFnr,
                    ),
                    barnaMedOpplysninger = listOf(
                        BarnMedOpplysninger(
                            ident = "",
                            erFolkeregistrert = false,
                            inkludertISøknaden = true,
                        ),
                    ),
                    endringAvOpplysningerBegrunnelse = "",
                ),
                bekreftEndringerViaFrontend = true,
            ),
        )

        val vedtaksperioder =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingEtterNySøknadsregistrering.id)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(1, vedtaksperioder.flatMap { it.eøsBegrunnelser }.size)
        assertEquals(
            EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN,
            vedtaksperioder.flatMap { it.eøsBegrunnelser }.first().begrunnelse,
        )
    }

    @Test
    fun `Skal kunne lagre flere vedtaksperioder av typen endret utbetaling med samme periode`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.REGISTRERE_SØKNAD,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(barnFnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        val fom = inneværendeMåned().minusMonths(12).førsteDagIInneværendeMåned()
        val tom = inneværendeMåned().sisteDagIInneværendeMåned()
        val type = Vedtaksperiodetype.UTBETALING
        val vedtaksperiode = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = fom,
            tom = tom,
            type = type,
        )
        vedtaksperiodeRepository.save(vedtaksperiode)

        val vedtaksperiodeMedSammePeriode = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = fom,
            tom = tom,
            type = type,
        )

        Assertions.assertDoesNotThrow {
            vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode)
        }
    }

    @Test
    fun `Skal validere at vedtaksperioder blir lagret ved fortsatt innvilget som resultat`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        assertEquals(Behandlingsresultat.FORTSATT_INNVILGET, revurdering.resultat)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(Vedtaksperiodetype.FORTSATT_INNVILGET, vedtaksperioder.first().type)
    }

    @Test
    fun `Skal legge til og overskrive begrunnelser og fritekst på vedtaksperiode`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )

        val vedtaksperioderMedUtfylteBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.size)
        assertEquals(
            Standardbegrunnelse.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
            vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.first().standardbegrunnelse,
        )

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.FORTSATT_INNVILGET_FAST_OMSORG),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )

        val vedtaksperioderMedOverskrevneBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.size)
        assertEquals(
            Standardbegrunnelse.FORTSATT_INNVILGET_FAST_OMSORG,
            vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.first().standardbegrunnelse,
        )
        assertEquals(0, vedtaksperioderMedOverskrevneBegrunnelser.first().fritekster.size)
    }

    @Test
    fun `Skal kaste feil når feil type blir valgt`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val feil = assertThrows<Feil> {
            vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                vedtaksperiodeId = vedtaksperioder.first().id,
                standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER),
                eøsStandardbegrunnelserFraFrontend = emptyList(),
            )
        }

        assertEquals(
            "Begrunnelsestype INNVILGET passer ikke med typen 'FORTSATT_INNVILGET' som er satt på perioden.",
            feil.message,
        )
    }

    @Test
    fun `skal identifisere reduserte perioder i begynnelsen`() {
        val barn1 = tilAktør(barnFnr)
        val barn2 = tilAktør(barn2Fnr)
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr, barn2Fnr))

        val forrigeAndelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn1,
        )
        val forrigeAndelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn2,
        )

        val andelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 5),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn2,
        )
        val vedtak = lagVedtak()
        val utbetalingsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 4, 1),
                tom = LocalDate.of(2021, 4, 30),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 5, 1),
                tom = LocalDate.of(2021, 8, 31),
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )

        val redusertePerioder = identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelTilkjentYtelse1, forrigeAndelTilkjentYtelse2),
            andelerTilkjentYtelse = listOf(andelTilkjentYtelse1, andelTilkjentYtelse2),
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = emptyList(),
            aktørerIForrigePersonopplysningGrunnlag = listOf(barn1, barn2),
        )
        assertTrue { redusertePerioder.isNotEmpty() }
        assertEquals(1, redusertePerioder.size)
        assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            redusertePerioder.first().type,
        )
        assertEquals(LocalDate.of(2021, 4, 1), redusertePerioder.first().fom)
        assertEquals(LocalDate.of(2021, 4, 30), redusertePerioder.first().tom)
    }

    @Test
    fun `skal ikke identifisere reduserte perioder midt i utbetalingsperiode når forrige måned har utbetaling`() {
        val barn1 = tilAktør(barnFnr)
        val barn2 = tilAktør(barn2Fnr)
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr, barn2Fnr))

        val forrigeAndelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn1,
        )
        val forrigeAndelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn2,
        )

        val andelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 6),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 8),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse3 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn2,
        )

        val vedtak = lagVedtak()
        val utbetalingsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 4, 1),
                tom = LocalDate.of(2021, 6, 30),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 7, 1),
                tom = LocalDate.of(2021, 8, 31),
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )

        val redusertePerioder = identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelTilkjentYtelse1, forrigeAndelTilkjentYtelse2),
            andelerTilkjentYtelse = listOf(andelTilkjentYtelse1, andelTilkjentYtelse2, andelTilkjentYtelse3),
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = emptyList(),
            aktørerIForrigePersonopplysningGrunnlag = listOf(barn1, barn2),
        )
        assertTrue { redusertePerioder.isEmpty() }
    }

    @Test
    fun `skal identifisere reduserte perioder midt i utbetalingsperiode når forrige måned ikke har utbetaling`() {
        val barn1 = tilAktør(barnFnr)
        val barn2 = tilAktør(barn2Fnr)
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr, barn2Fnr))

        val forrigeAndelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn1,
        )
        val forrigeAndelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn2,
        )

        val andelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 4),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 8),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse3 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 4),
            behandling = behandling,
            aktør = barn2,
        )
        val andelTilkjentYtelse4 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 6),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn2,
        )

        val vedtak = lagVedtak()
        val opphørsperiode = lagVedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.of(2021, 5, 1),
            tom = LocalDate.of(2021, 5, 31),
            type = Vedtaksperiodetype.OPPHØR,
        )
        val utbetalingsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 4, 1),
                tom = LocalDate.of(2021, 4, 30),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 6, 1),
                tom = LocalDate.of(2021, 8, 31),
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )

        val redusertePerioder = identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelTilkjentYtelse1, forrigeAndelTilkjentYtelse2),
            andelerTilkjentYtelse = listOf(
                andelTilkjentYtelse1,
                andelTilkjentYtelse2,
                andelTilkjentYtelse3,
                andelTilkjentYtelse4,
            ),
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = listOf(opphørsperiode),
            aktørerIForrigePersonopplysningGrunnlag = listOf(barn1, barn2),
        )
        assertTrue { redusertePerioder.isNotEmpty() }
        assertEquals(1, redusertePerioder.size)
        assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            redusertePerioder.first().type,
        )
        assertEquals(LocalDate.of(2021, 6, 1), redusertePerioder.first().fom)
        assertEquals(LocalDate.of(2021, 7, 31), redusertePerioder.first().tom)
    }

    @Test
    fun `skal identifisere flere reduserte perioder`() {
        val barn1 = tilAktør(barnFnr)
        val barn2 = tilAktør(barn2Fnr)
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr, barn2Fnr))

        val forrigeAndelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn1,
        )
        val forrigeAndelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 8),
            aktør = barn2,
        )

        val andelTilkjentYtelse1 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 4),
            tom = YearMonth.of(2021, 4),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 8),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn1,
        )
        val andelTilkjentYtelse3 = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.of(2021, 6),
            tom = YearMonth.of(2021, 8),
            behandling = behandling,
            aktør = barn2,
        )

        val vedtak = lagVedtak()
        val opphørsperiode = lagVedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.of(2021, 5, 1),
            tom = LocalDate.of(2021, 5, 31),
            type = Vedtaksperiodetype.OPPHØR,
        )
        val utbetalingsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 4, 1),
                tom = LocalDate.of(2021, 4, 30),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.of(2021, 6, 1),
                tom = LocalDate.of(2021, 8, 31),
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )

        val redusertePerioder = identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = listOf(forrigeAndelTilkjentYtelse1, forrigeAndelTilkjentYtelse2),
            andelerTilkjentYtelse = listOf(
                andelTilkjentYtelse1,
                andelTilkjentYtelse2,
                andelTilkjentYtelse3,
            ),
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = listOf(opphørsperiode),
            aktørerIForrigePersonopplysningGrunnlag = listOf(barn1, barn2),
        )
        assertTrue { redusertePerioder.isNotEmpty() }
        assertEquals(2, redusertePerioder.size)
        assertTrue { redusertePerioder.all { Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING == it.type } }
        assertEquals(LocalDate.of(2021, 4, 1), redusertePerioder.first().fom)
        assertEquals(LocalDate.of(2021, 4, 30), redusertePerioder.first().tom)
        assertEquals(LocalDate.of(2021, 6, 1), redusertePerioder.last().fom)
        assertEquals(LocalDate.of(2021, 7, 31), redusertePerioder.last().tom)
    }

    @Test
    fun `generere vedtaksperioder basert på manuelt overstyrt endringstidspunkt`() {
        val vedtak = lagVedtak()
        val avslagsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 4, 30),
                Vedtaksperiodetype.AVSLAG,
            ),
        )
        val utbetalingsperioder = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 2, 28),
                Vedtaksperiodetype.UTBETALING,
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak,
                LocalDate.of(2021, 3, 1),
                LocalDate.of(2021, 7, 31),
                Vedtaksperiodetype.UTBETALING,
            ),
        )
        val vedtaksperioder = utbetalingsperioder.filtrerLikEllerEtterEndringstidspunkt(
            endringstidspunkt = LocalDate.of(2021, 3, 1),
        ) + avslagsperioder

        assertNotNull(vedtaksperioder)
        assertEquals(2, vedtaksperioder.size)
        assertTrue {
            vedtaksperioder.any {
                it.fom == LocalDate.of(2021, 1, 1) &&
                    it.tom == LocalDate.of(2021, 4, 30) &&
                    it.type == Vedtaksperiodetype.AVSLAG
            }
        }
        assertTrue {
            vedtaksperioder.any {
                it.fom == LocalDate.of(2021, 3, 1) &&
                    it.tom == LocalDate.of(2021, 7, 31) &&
                    it.type == Vedtaksperiodetype.UTBETALING
            }
        }
    }
}
