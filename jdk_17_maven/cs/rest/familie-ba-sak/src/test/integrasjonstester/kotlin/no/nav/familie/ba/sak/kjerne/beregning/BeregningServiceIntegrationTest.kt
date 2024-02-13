package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class BeregningServiceIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    private lateinit var beregningService: BeregningService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var personidentService: PersonidentService

    @Autowired
    private lateinit var aktørIdRepository: AktørIdRepository

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForFGBMedToBarn() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val fomBarn1 = dagensDato.withDayOfMonth(1)
        val fomBarn2 = fomBarn1.plusYears(2)
        val tomBarn1 = fomBarn1.plusYears(18).sisteDagIMåned()
        val tomBarn2 = fomBarn2.plusYears(18).sisteDagIMåned()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGBMedToBarn(
            fnr,
            fagsak.id.toString(),
            behandling.id,
            dagensDato,
            fomBarn1,
            tomBarn1,
            fomBarn2,
            tomBarn2,
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            fomBarn1.toYearMonth(),
            tomBarn1.toYearMonth(),
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            fomBarn2.toYearMonth(),
            tomBarn2.toYearMonth(),
        )

        val tilkjentYtelse =
            beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(fomBarn1.toYearMonth(), tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomBarn2.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertNull(tilkjentYtelse.opphørFom)
    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForOpphørMedToBarn() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val fomBarn1 = dagensDato.withDayOfMonth(1)
        val fomBarn2 = fomBarn1.plusYears(2)
        val tomBarn1 = fomBarn1.plusYears(18).sisteDagIMåned()
        val tomBarn2 = fomBarn2.plusYears(18).sisteDagIMåned()
        val opphørsDato = fomBarn1.plusYears(5).withDayOfMonth(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForOpphørMedToBarn(
            fnr,
            fagsak.id.toString(),
            behandling.id,
            dagensDato,
            fomBarn1,
            tomBarn1,
            fomBarn2,
            tomBarn2,
            opphørsDato,
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            fomBarn1.toYearMonth(),
            tomBarn1.toYearMonth(),
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            fomBarn2.toYearMonth(),
            tomBarn2.toYearMonth(),
        )

        val tilkjentYtelse =
            beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertNull(tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomBarn2.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertNotNull(tilkjentYtelse.opphørFom)
        Assertions.assertEquals(opphørsDato.toYearMonth(), tilkjentYtelse.opphørFom)
    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForRevurderingMedToBarn() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val opphørFomBarn1 = LocalDate.of(2020, 5, 1)
        val revurderingFomBarn1 = LocalDate.of(2020, 7, 1)
        val fomDatoBarn1 = LocalDate.of(2020, 1, 1)
        val tomDatoBarn1 = fomDatoBarn1.plusYears(18).sisteDagIForrigeMåned()

        val opphørFomBarn2 = LocalDate.of(2020, 8, 1)
        val revurderingFomBarn2 = LocalDate.of(2020, 10, 1)
        val fomDatoBarn2 = LocalDate.of(2019, 10, 1)
        val tomDatoBarn2 = fomDatoBarn2.plusYears(18).sisteDagIForrigeMåned()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING),
        )
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForRevurderingMedToBarn(
            fnr,
            fagsak.id.toString(),
            behandling.id,
            behandling.id - 1,
            dagensDato,
            opphørFomBarn1,
            revurderingFomBarn1,
            fomDatoBarn1,
            tomDatoBarn1,
            opphørFomBarn2,
            revurderingFomBarn2,
            fomDatoBarn2,
            tomDatoBarn2,
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            revurderingFomBarn1.toYearMonth(),
            tomDatoBarn1.toYearMonth(),
        )

        leggTilAndelTilkjentYtelsePåTilkjentYtelse(
            behandling,
            revurderingFomBarn2.toYearMonth(),
            tomDatoBarn2.toYearMonth(),
        )

        val tilkjentYtelse =
            beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(revurderingFomBarn1.toYearMonth(), tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomDatoBarn1.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertEquals(opphørFomBarn2.toYearMonth(), tilkjentYtelse.opphørFom)
    }

    @Test
    fun `Skal lagre andelerTilkjentYtelse med kobling til TilkjentYtelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val søkerAktørId = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barn1AktørId = personidentService.hentOgLagreAktør(barn1Fnr, true)
        val barn2AktørId = personidentService.hentOgLagreAktør(barn2Fnr, true)
        val dato_2021_11_01 = LocalDate.of(2021, 11, 1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn1Fnr, barn2Fnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val barn1Id =
            personopplysningGrunnlag.barna.find { it.aktør.aktivFødselsnummer() == barn1Fnr }!!.aktør.aktivFødselsnummer()
        val barn2Id =
            personopplysningGrunnlag.barna.find { it.aktør.aktivFødselsnummer() == barn2Fnr }!!.aktør.aktivFødselsnummer()

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(
            vilkårsvurdering,
            søkerAktørId,
            barn1AktørId,
            barn2AktørId,
            dato_2021_11_01,
            dato_2021_11_01.plusYears(17),
        )
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val andelBarn1 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn1Id }
        val andelBarn2 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn2Id }

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
        Assertions.assertEquals(3, andelBarn1.size)
        Assertions.assertEquals(3, andelBarn2.size)
        tilkjentYtelse.andelerTilkjentYtelse.forEach {
            Assertions.assertEquals(tilkjentYtelse, it.tilkjentYtelse)
        }
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1654 }.size)
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1676 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1654 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1676 }.size)
    }

    @Nested
    inner class HentSisteAndelPerIdent {

        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        val aktørSøker = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val aktørBarn1 = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)
        val aktørBarn2 = personidentService.hentOgLagreAktør(barn2.aktør.aktivFødselsnummer(), true)

        lateinit var førsteBehandling: Behandling

        @BeforeEach
        fun setUp() {
            førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        }

        @Test
        fun `ingen andeler`() {
            assertThat(hentSisteAndelPerIdent()).isEmpty()
        }

        @Test
        fun `uten utbetalingsoppdrag`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = null)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).isEmpty()
        }

        @Test
        fun `behandling er ikke avsluttet`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).isEmpty()
        }

        @Test
        fun `gitt fagsak har ikke noen andeler`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            assertThat(beregningService.hentSisteAndelPerIdent(fagsak.id + 1)).isEmpty()
        }

        @Test
        fun `2 ulike personer med samme type`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn2,
                        person = barn2,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            with(sisteAndelPerIdent[IdentOgYtelse(barn1.aktør.aktivFødselsnummer(), YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(0L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 1))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 2))
            }
            with(sisteAndelPerIdent[IdentOgYtelse(barn2.aktør.aktivFødselsnummer(), YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
            }
        }

        @Test
        fun `førstegångsbehandling med flere andeler per person`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 3),
                        offset = 2,
                        forrigeOffset = null,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
            }
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.UTVIDET_BARNETRYGD)]!!) {
                assertThat(periodeOffset).isEqualTo(2L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 3))
            }
        }

        @Test
        fun `siste andelen kommer fra revurderingen`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val revurdering = lagRevurdering()
            with(lagInitiellTilkjentYtelse(revurdering, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(revurdering)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 1))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(kildeBehandlingId).isEqualTo(revurdering.id)
            }
        }

        @Test
        fun `en revurdering opphører en andel, sånn at siste andelen finnes i en tidligere behandling`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 4),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val revurdering = lagRevurdering()
            with(lagInitiellTilkjentYtelse(revurdering, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(revurdering)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 4))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
                assertThat(kildeBehandlingId).isEqualTo(førsteBehandling.id)
            }
        }

        fun hentSisteAndelPerIdent(): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
            return beregningService.hentSisteAndelPerIdent(fagsak.id)
        }

        fun lagAndel(
            tilkjentYtelse: TilkjentYtelse,
            ytelseType: YtelseType = YtelseType.SMÅBARNSTILLEGG,
            person: Person? = null,
            aktør: Aktør? = null,
            fom: YearMonth,
            tom: YearMonth,
            offset: Long,
            forrigeOffset: Long? = null,
        ): AndelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                fom,
                tom,
                ytelseType,
                1345,
                tilkjentYtelse.behandling,
                person = person ?: søker,
                aktør = aktør ?: aktørSøker,
                tilkjentYtelse = tilkjentYtelse,
                periodeIdOffset = offset,
                forrigeperiodeIdOffset = forrigeOffset,
            )

        private fun lagRevurdering() = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(fagsak, behandlingType = BehandlingType.REVURDERING),
        )
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, BehandlingStatus.AVSLUTTET)
    }

    private fun opprettTilkjentYtelse(behandling: Behandling) {
        tilkjentYtelseRepository.saveAndFlush(lagInitiellTilkjentYtelse(behandling))
    }

    private fun leggTilAndelTilkjentYtelsePåTilkjentYtelse(behandling: Behandling, fom: YearMonth, tom: YearMonth) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val tilfeldigperson = tilfeldigPerson(aktør = tilAktør(randomFnr()))
        aktørIdRepository.saveAndFlush(tilfeldigperson.aktør)

        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            fom,
            tom,
            YtelseType.ORDINÆR_BARNETRYGD,
            1054,
            behandling,
            tilkjentYtelse = tilkjentYtelse,
            aktør = tilfeldigperson.aktør,
        )

        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
    }
}
