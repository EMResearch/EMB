package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SatsendringUtilTest {

    private val UGYLDIG_SATS = 1000

    @Test
    fun `Skal returnere true dersom vi har siste sats`() {
        val andelerMedSisteSats = SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .map {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp,
                    ytelseType = it.tilYtelseType(),
                )
            }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser())
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats selv om alle perioder er fram i tid`() {
        val andelerMedSisteSats = SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .map {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    sats = sisteSats.beløp,
                    ytelseType = it.tilYtelseType(),
                )
            }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser())
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        sats = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType(),
                    ),
                )

                assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatser())
            }
    }

    @Test
    fun `Skal ignorere andeler som kommer før siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerSomErFørSisteSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth().minusMonths(100),
                        tom = sisteSats.gyldigFom.toYearMonth().minusMonths(1),
                        sats = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType(),
                    ),
                )

                assertTrue(andelerSomErFørSisteSats.erOppdatertMedSisteSatser())
            }
    }

    @Test
    fun `Skal ikke returnere false dersom vi ikke har siste sats, men de er redusert til 0 prosent`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        sats = sisteSats.beløp - 1,
                        prosent = BigDecimal.ZERO,
                        ytelseType = it.tilYtelseType(),
                    ),
                )

                assertTrue(andelerMedFeilSats.erOppdatertMedSisteSatser())
            }
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis den har siste satsendring`() {
        val behandling = lagBehandling()
        val atyMedBareSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
            )

        Assertions.assertThat(atyMedBareSmåbarnstillegg.erOppdatertMedSisteSatser()).isEqualTo(true)

        val atyMedBareUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
            )

        Assertions.assertThat(atyMedBareUtvidet.erOppdatertMedSisteSatser()).isEqualTo(true)

        val atyMedBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
            )

        Assertions.assertThat(atyMedBareOrba.erOppdatertMedSisteSatser()).isEqualTo(true)

        val atyMedBareTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.TILLEGG_ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
            )

        Assertions.assertThat(atyMedBareTilleggOrba.erOppdatertMedSisteSatser()).isEqualTo(true)

        Assertions.assertThat(
            (atyMedBareTilleggOrba + atyMedBareOrba + atyMedBareUtvidet + atyMedBareSmåbarnstillegg)
                .erOppdatertMedSisteSatser(),
        ).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false hvis den har gammel satsendring`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
                UGYLDIG_SATS,
            )

        Assertions.assertThat(atyMedUgyldigSatsSmåbarnstillegg.erOppdatertMedSisteSatser()).isEqualTo(false)

        val atyMedUglydligSatsUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
                UGYLDIG_SATS,
            )

        Assertions.assertThat(atyMedUglydligSatsUtvidet.erOppdatertMedSisteSatser()).isEqualTo(false)

        val atyMedUgyldigSatsBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                UGYLDIG_SATS,
            )

        Assertions.assertThat(atyMedUgyldigSatsBareOrba.erOppdatertMedSisteSatser()).isEqualTo(false)

        val atyMedUgyldigSatsTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.TILLEGG_ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                UGYLDIG_SATS,
            )

        Assertions.assertThat(atyMedUgyldigSatsTilleggOrba.erOppdatertMedSisteSatser()).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false en av satsene ikke er ny`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
                UGYLDIG_SATS,
            )

        val atyMedGyldigUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
            )

        val atyMedBGyldigOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
            )

        Assertions.assertThat(
            (atyMedBGyldigOrba + atyMedGyldigUtvidet + atyMedUgyldigSatsSmåbarnstillegg).erOppdatertMedSisteSatser(),
        ).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true på ytelse med rett sats når tom dato er på samme dato som satstidspunkt`() {
        val behandling = lagBehandling()
        val atySomGårUtPåSatstidspunktGyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        Assertions.assertThat(listOf(atySomGårUtPåSatstidspunktGyldig).erOppdatertMedSisteSatser()).isEqualTo(true)

        val atySomGårUtPåSatstidspunktUgyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = UGYLDIG_SATS,
            )

        Assertions.assertThat(listOf(atySomGårUtPåSatstidspunktUgyldig).erOppdatertMedSisteSatser()).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis ingen aktive andel tilkjent ytelser`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(10),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser()).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true for ny sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).plusYears(10),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser()).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false for gammel sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).plusYears(10),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = UGYLDIG_SATS,
            )

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser()).isEqualTo(false)
    }

    private fun lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
        satsType: SatsType,
        behandling: Behandling,
        ytelseType: YtelseType,
        beløp: Int? = null,
    ) = listOf(
        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = SatsService.finnSisteSatsFor(satsType).gyldigFom.minusMonths(1).toYearMonth(),
            tom = SatsService.finnSisteSatsFor(satsType).gyldigFom.plusMonths(1).toYearMonth(),
            ytelseType = ytelseType,
            behandling = behandling,
            person = lagPerson(),
            aktør = lagPerson().aktør,
            periodeIdOffset = 1,
            beløp = beløp ?: SatsService.finnSisteSatsFor(satsType).beløp,
        ),
    )

    private fun datoForSisteSatsendringForSatsType(satsType: SatsType) =
        SatsService.finnSisteSatsFor(satsType).gyldigFom.toYearMonth()
}
