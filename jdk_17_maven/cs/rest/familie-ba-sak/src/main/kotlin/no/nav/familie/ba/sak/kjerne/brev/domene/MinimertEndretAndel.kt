package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

class MinimertEndretAndel(
    val aktørId: String,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val prosent: BigDecimal?,
) {
    fun månedPeriode() = MånedPeriode(fom!!, tom!!)

    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.fom!!,
            this.tom!!,
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth(),
            ),
        )
    }
}

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel(): MinimertEndretAndel {
    this.validerUtfyltEndring()

    return MinimertEndretAndel(
        fom = this.fom!!,
        tom = this.tom!!,
        aktørId = this.person?.aktør?.aktørId ?: throw Feil(
            "Finner ikke aktørId på endretUtbetalingsandel ${this.id} " +
                "ved konvertering til minimertEndretUtbetalingsandel",
        ),
        årsak = this.årsak ?: throw Feil(
            "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
                "ved konvertering til minimertEndretUtbetalingsandel",
        ),
        prosent = this.prosent,
    )
}
