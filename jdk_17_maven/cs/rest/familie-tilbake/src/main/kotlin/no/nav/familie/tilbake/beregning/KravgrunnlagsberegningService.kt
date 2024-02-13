package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.isNotZero
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.function.Function

@Service
object KravgrunnlagsberegningService {

    private val feilutbetaltYtelsesbeløputleder: (Kravgrunnlagsperiode432) -> BigDecimal = { kgPeriode: Kravgrunnlagsperiode432 ->
        kgPeriode.beløp
            .filter { it.klassetype == Klassetype.FEIL }
            .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
    }

    private val utbetaltYtelsesbeløputleder = { kgPeriode: Kravgrunnlagsperiode432 ->
        kgPeriode.beløp
            .filter { it.klassetype == Klassetype.YTEL }
            .sumOf(Kravgrunnlagsbeløp433::opprinneligUtbetalingsbeløp)
    }

    private val riktigYteslesbeløputleder = { kgPeriode: Kravgrunnlagsperiode432 ->
        kgPeriode.beløp
            .filter { it.klassetype == Klassetype.YTEL }
            .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
    }

    fun fordelKravgrunnlagBeløpPåPerioder(
        kravgrunnlag: Kravgrunnlag431,
        vurderingsperioder: List<Månedsperiode>,
    ): Map<Månedsperiode, FordeltKravgrunnlagsbeløp> {
        return vurderingsperioder.associateWith {
            FordeltKravgrunnlagsbeløp(
                beregnBeløp(kravgrunnlag, it, feilutbetaltYtelsesbeløputleder),
                beregnBeløp(kravgrunnlag, it, utbetaltYtelsesbeløputleder),
                beregnBeløp(kravgrunnlag, it, riktigYteslesbeløputleder),
            )
        }
    }

    fun summerKravgrunnlagBeløpForPerioder(kravgrunnlag: Kravgrunnlag431): Map<Månedsperiode, FordeltKravgrunnlagsbeløp> {
        return kravgrunnlag.perioder.associate {
            it.periode to FordeltKravgrunnlagsbeløp(
                feilutbetaltYtelsesbeløputleder(it),
                utbetaltYtelsesbeløputleder(it),
                riktigYteslesbeløputleder(it),
            )
        }
    }

    fun beregnFeilutbetaltBeløp(kravgrunnlag: Kravgrunnlag431, vurderingsperiode: Månedsperiode): BigDecimal {
        return beregnBeløp(kravgrunnlag, vurderingsperiode, feilutbetaltYtelsesbeløputleder)
    }

    fun validatePerioder(perioder: List<Datoperiode>) {
        val perioderSomIkkeErHeleMåneder = perioder.filter {
            it.fom.dayOfMonth != 1 ||
                it.tom.dayOfMonth != YearMonth.from(it.tom).lengthOfMonth()
        }

        if (perioderSomIkkeErHeleMåneder.isNotEmpty()) {
            throw Feil(
                message = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
                frontendFeilmelding = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun beregnBeløp(
        kravgrunnlag: Kravgrunnlag431,
        vurderingsperiode: Månedsperiode,
        beløpsummerer: Function<Kravgrunnlagsperiode432, BigDecimal>,
    ): BigDecimal {
        val sum = kravgrunnlag.perioder
            .sortedBy { it.periode.fom }
            .sumOf {
                val beløp = beløpsummerer.apply(it)
                if (beløp.isNotZero()) {
                    val beløpPerMåned: BigDecimal = BeløpsberegningUtil.beregnBeløpPerMåned(beløp, it.periode)
                    BeløpsberegningUtil.beregnBeløp(vurderingsperiode, it.periode, beløpPerMåned)
                } else {
                    BigDecimal.ZERO
                }
            }
        return sum.setScale(0, RoundingMode.HALF_UP)
    }
}
