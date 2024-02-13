package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Enkel DSL for å bygge TilkjentYtelse. Eksempel på bruk er:
 *
 * val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling) der
 *     (søker har 1054 i UTVIDET_BARNETRYGD fom jun(2018) tom jul(2024)) og
 *     (søker har 660 i SMÅBARNSTILLEGG fom aug(2018) tom des(2019)) og
 *     (søker har 660 i SMÅBARNSTILLEGG fom jul(2020) tom mai(2023)) og
 *     (barn1 har 1054 i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022)) og
 *     (barn1 har 1054 i ORDINÆR_BARNETRYGD fom aug(2022) tom jul(2024)) og
 *     (barn2 har 1054 i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))
 *
 *     Utenlandsk beløp som gir differanseberegning kan introduseres med <og> eller <minus>, f.eks:
 *     (barn1 har 1054 og 756 i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022))
 *     (barn1 har 1054 minus 756 i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022))
 */
infix fun TilkjentYtelse.der(andelTilkjentYtelse: AndelTilkjentYtelse): TilkjentYtelse {
    this.andelerTilkjentYtelse.add(
        andelTilkjentYtelse.copy(
            tilkjentYtelse = this,
            behandlingId = this.behandling.id,
        ),
    )
    return this
}

infix fun TilkjentYtelse.og(andelTilkjentYtelse: AndelTilkjentYtelse) = this.der(andelTilkjentYtelse)

infix fun Person.har(sats: Int) = AndelTilkjentYtelse(
    aktør = this.aktør,
    sats = sats,
    kalkulertUtbetalingsbeløp = sats,
    behandlingId = 0,
    tilkjentYtelse = lagInitiellTilkjentYtelse(),
    stønadFom = YearMonth.now(),
    stønadTom = YearMonth.now(),
    type = YtelseType.ORDINÆR_BARNETRYGD,
    prosent = BigDecimal.valueOf(100),
    nasjonaltPeriodebeløp = sats,
)

infix fun AndelTilkjentYtelse.fom(tidspunkt: Tidspunkt<Måned>) = this.copy(stønadFom = tidspunkt.tilYearMonth())
infix fun AndelTilkjentYtelse.tom(tidspunkt: Tidspunkt<Måned>) = this.copy(stønadTom = tidspunkt.tilYearMonth())
infix fun AndelTilkjentYtelse.i(ytelseType: YtelseType) = this.copy(type = ytelseType)
infix fun AndelTilkjentYtelse.og(utenlandskBeløp: Int) = this.copy(
    differanseberegnetPeriodebeløp = sats - utenlandskBeløp,
    kalkulertUtbetalingsbeløp = maxOf(sats - utenlandskBeløp, 0),
)

infix fun AndelTilkjentYtelse.minus(utenlandskBeløp: Int) = this.og(utenlandskBeløp)
