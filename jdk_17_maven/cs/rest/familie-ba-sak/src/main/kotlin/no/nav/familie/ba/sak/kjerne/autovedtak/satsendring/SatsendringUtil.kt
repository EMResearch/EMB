package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import java.math.BigDecimal

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertMedSisteSatser(): Boolean =
    SatsType.values()
        .filter { it != SatsType.FINN_SVAL }
        .all { this.erOppdatertFor(it) }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.erOppdatertFor(satstype: SatsType): Boolean {
    val sisteSatsForSatstype = SatsService.finnSisteSatsFor(satstype)
    val fomSisteSatsForSatstype = sisteSatsForSatstype.gyldigFom.toYearMonth()

    val satsTyperMedTilsvarendeYtelsestype = satstype
        .tilYtelseType()
        .hentSatsTyper()

    return this.filter { it.stønadTom.isSameOrAfter(fomSisteSatsForSatstype) }
        .filter { it.type == satstype.tilYtelseType() }
        .filter { it.prosent != BigDecimal.ZERO }
        .all { andelTilkjentYtelse ->
            satsTyperMedTilsvarendeYtelsestype
                .any { andelTilkjentYtelse.sats == SatsService.finnSisteSatsFor(it).beløp }
        }
}
