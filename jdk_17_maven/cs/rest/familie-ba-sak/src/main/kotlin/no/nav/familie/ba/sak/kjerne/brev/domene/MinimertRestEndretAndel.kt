package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder personIdent og ikke aktørId.
 */
data class MinimertRestEndretAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
    val søknadstidspunkt: LocalDate,
    val avtaletidspunktDeltBosted: LocalDate?,
) {
    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        return MånedPeriode(
            this.periode.fom,
            this.periode.tom,
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom ?: TIDENES_MORGEN.toYearMonth(),
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth(),
            ),
        )
    }
}

fun List<MinimertRestEndretAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.tilMinimertRestEndretUtbetalingAndel() = MinimertRestEndretAndel(
    periode = this.periode,
    personIdent = this.person?.aktør?.aktivFødselsnummer() ?: throw Feil(
        "Har ikke ident på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel",
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel",
    ),
    søknadstidspunkt = this.søknadstidspunkt ?: throw Feil(
        "Har ikke søknadstidspunk på endretUtbetalingsandel  ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel",
    ),
    avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted ?: (
        if (this.årsakErDeltBosted()) {
            throw Feil(
                "Har ikke avtaletidspunktDeltBosted på endretUtbetalingsandel  ${this.id} " +
                    "ved konvertering til minimertRestEndretUtbetalingsandel",
            )
        } else {
            null
        }
        ),
)
