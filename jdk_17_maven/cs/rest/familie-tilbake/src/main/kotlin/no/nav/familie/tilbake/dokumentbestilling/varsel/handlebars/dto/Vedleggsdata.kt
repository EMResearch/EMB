package no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte
import java.math.BigDecimal
import java.time.YearMonth

class Vedleggsdata(
    override val språkkode: Språkkode,
    @Suppress("unused") // Handlebars
    val ytelseMedSkatt: Boolean,
    val feilutbetaltePerioder: List<FeilutbetaltPeriode>,
) : Språkstøtte

data class FeilutbetaltPeriode(
    val måned: YearMonth,
    val nyttBeløp: BigDecimal,
    val tidligereUtbetaltBeløp: BigDecimal,
    val feilutbetaltBeløp: BigDecimal,
)
