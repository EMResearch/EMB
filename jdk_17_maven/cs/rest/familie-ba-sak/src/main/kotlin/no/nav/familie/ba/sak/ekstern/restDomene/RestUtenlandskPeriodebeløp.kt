package no.nav.familie.ba.sak.ekstern.restDomene

import jakarta.validation.constraints.DecimalMin
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.konverterBeløpTilMånedlig
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.YearMonth

data class RestUtenlandskPeriodebeløp(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    @field:DecimalMin(value = "0.0", message = "Beløp kan ikke være negativt.") val beløp: BigDecimal?,
    val valutakode: String?,
    val intervall: Intervall?,
    val kalkulertMånedligBeløp: BigDecimal?,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT,
) : AbstractUtfyltStatus<RestUtenlandskPeriodebeløp>() {
    override fun medUtfyltStatus(): RestUtenlandskPeriodebeløp {
        return this.copy(
            status = utfyltStatus(
                finnAntallUtfylt(listOf(this.beløp, this.valutakode, this.intervall)),
                3,
            ),
        )
    }
}

fun RestUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(
    barnAktører: List<Aktør>,
    eksisterendeUtenlandskPeriodebeløp: UtenlandskPeriodebeløp,
) = UtenlandskPeriodebeløp(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    utbetalingsland = eksisterendeUtenlandskPeriodebeløp.utbetalingsland,
    kalkulertMånedligBeløp = this.tilKalkulertMånedligBeløp(),
)

fun RestUtenlandskPeriodebeløp.tilKalkulertMånedligBeløp(): BigDecimal? {
    if (this.beløp == null || this.intervall == null) {
        return null
    }

    return this.intervall.konverterBeløpTilMånedlig(this.beløp)
}

fun UtenlandskPeriodebeløp.tilKalkulertMånedligBeløp(): BigDecimal? {
    if (this.beløp == null || this.intervall == null) {
        return null
    }

    return this.intervall.konverterBeløpTilMånedlig(this.beløp)
}

fun UtenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp() = RestUtenlandskPeriodebeløp(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    kalkulertMånedligBeløp = this.kalkulertMånedligBeløp,
).medUtfyltStatus()
