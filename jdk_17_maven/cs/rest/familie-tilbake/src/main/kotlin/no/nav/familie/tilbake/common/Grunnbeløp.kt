package no.nav.familie.tilbake.common

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal

object Grunnbeløpsperioder {

    fun finnGrunnbeløpsperioder(periode: Månedsperiode): List<Grunnbeløp> {
        require(periode.tom <= grunnbeløpsperioderMaksTom) {
            "Har ikke lagt inn grunnbeløpsperiode frem til ${periode.tom}"
        }
        val perioder = grunnbeløpsperioder.filter {
            it.periode.overlapper(periode)
        }
        require(perioder.isNotEmpty()) {
            "Forventer å finne treff for ${periode.fom} - ${periode.tom} i grunnbeløpsperioder"
        }
        return perioder.sortedBy { it.periode }
    }
}

data class Grunnbeløp(
    val periode: Månedsperiode,
    val grunnbeløp: BigDecimal,
    val perMnd: BigDecimal,
    val gjennomsnittPerÅr: BigDecimal? = null,
)

// Kopiert inn fra https://github.com/navikt/g
private val grunnbeløpsperioder: List<Grunnbeløp> =
    listOf(
        Grunnbeløp(
            periode = Månedsperiode("2023-05" to "2024-04"), // Setter ikke MAX for å unngå at grunnbeløpet ikke er oppdatert for neste periode
            grunnbeløp = 118_620.toBigDecimal(),
            perMnd = 9_885.toBigDecimal(),
            gjennomsnittPerÅr = 116239.toBigDecimal(),
        ),
        Grunnbeløp(
            periode = Månedsperiode("2022-05" to "2023-04"),
            grunnbeløp = 111_477.toBigDecimal(),
            perMnd = 9_290.toBigDecimal(),
            gjennomsnittPerÅr = 109_784.toBigDecimal(),
        ),
        Grunnbeløp(
            periode = Månedsperiode("2021-05" to "2022-04"),
            grunnbeløp = 106_399.toBigDecimal(),
            perMnd = 8_867.toBigDecimal(),
            gjennomsnittPerÅr = 104_716.toBigDecimal(),
        ),
        Grunnbeløp(
            periode = Månedsperiode("2020-05" to "2021-04"),
            grunnbeløp = 101_351.toBigDecimal(),
            perMnd = 8_446.toBigDecimal(),
            gjennomsnittPerÅr = 100_853.toBigDecimal(),
        ),
        Grunnbeløp(
            periode = Månedsperiode("2019-05" to "2020-04"),
            grunnbeløp = 99_858.toBigDecimal(),
            perMnd = 8_322.toBigDecimal(),
            gjennomsnittPerÅr = 98_866.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2018-05" to "2019-04"),
            grunnbeløp = 96_883.toBigDecimal(),
            perMnd = 8_074.toBigDecimal(),
            gjennomsnittPerÅr = 95_800.toBigDecimal(),
        ),
        Grunnbeløp(
            periode = Månedsperiode("2017-05" to "2018-04"),
            grunnbeløp = 93_634.toBigDecimal(),
            perMnd = 7_803.toBigDecimal(),
            gjennomsnittPerÅr = 93_281.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2016-05" to "2017-04"),
            grunnbeløp = 92_576.toBigDecimal(),
            perMnd = 7_715.toBigDecimal(),
            gjennomsnittPerÅr = 91_740.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2015-05" to "2016-04"),
            grunnbeløp = 90_068.toBigDecimal(),
            perMnd = 7_506.toBigDecimal(),
            gjennomsnittPerÅr = 89_502.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2014-05" to "2015-04"),
            grunnbeløp = 88_370.toBigDecimal(),
            perMnd = 7_364.toBigDecimal(),
            gjennomsnittPerÅr = 87_328.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2013-05" to "2014-04"),
            grunnbeløp = 85_245.toBigDecimal(),
            perMnd = 7_104.toBigDecimal(),
            gjennomsnittPerÅr = 84_204.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2012-05" to "2013-04"),
            grunnbeløp = 82_122.toBigDecimal(),
            perMnd = 6_844.toBigDecimal(),
            gjennomsnittPerÅr = 81_153.toBigDecimal(),

        ),
        Grunnbeløp(
            periode = Månedsperiode("2011-05" to "2012-04"),
            grunnbeløp = 79_216.toBigDecimal(),
            perMnd = 6_601.toBigDecimal(),
            gjennomsnittPerÅr = 78_024.toBigDecimal(),

        ),
    )

private val grunnbeløpsperioderMaksTom = grunnbeløpsperioder.maxOf { it.periode.tom }
