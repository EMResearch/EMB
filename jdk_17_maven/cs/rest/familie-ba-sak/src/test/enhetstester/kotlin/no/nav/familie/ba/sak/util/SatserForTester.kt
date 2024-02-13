package no.nav.familie.ba.sak.util

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import java.time.LocalDate

fun tilleggOrdinærSatsTilTester(): Int =
    SatsService.finnAlleSatserFor(SatsType.TILLEGG_ORBA).findLast {
        it.gyldigFom <= LocalDate.now().plusDays(1)
    }!!.beløp

fun sisteUtvidetSatsTilTester(): Int = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp

fun sisteSmåbarnstilleggSatsTilTester(): Int = SatsService.finnSisteSatsFor(SatsType.SMA).beløp

fun sisteTilleggOrdinærSats(): Double = SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp.toDouble()

fun tilleggOrdinærSatsNesteMånedTilTester(): Sats =
    SatsService.finnAlleSatserFor(SatsType.TILLEGG_ORBA).findLast {
        it.gyldigFom.toYearMonth() <= LocalDate.now().nesteMåned()
    }!!
