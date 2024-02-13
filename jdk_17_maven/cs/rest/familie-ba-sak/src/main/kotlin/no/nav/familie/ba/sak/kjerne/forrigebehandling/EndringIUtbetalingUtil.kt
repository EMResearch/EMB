package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import java.time.YearMonth

object EndringIUtbetalingUtil {

    fun utledEndringstidspunktForUtbetalingsbeløp(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): YearMonth? {
        val endringIUtbetalingTidslinje = lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
        )

        return endringIUtbetalingTidslinje.tilFørsteEndringstidspunkt()
    }

    internal fun lagEndringIUtbetalingTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean, Måned> {
        val allePersonerMedAndeler = (nåværendeAndeler.map { it.aktør } + forrigeAndeler.map { it.aktør }).distinct()

        val endringstidslinjePerPersonOgType = allePersonerMedAndeler.flatMap { aktør ->
            val ytelseTyperForPerson = (nåværendeAndeler.map { it.type } + forrigeAndeler.map { it.type }).distinct()

            ytelseTyperForPerson.map { ytelseType ->
                lagEndringIUtbetalingForPersonOgTypeTidslinje(
                    nåværendeAndeler = nåværendeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    forrigeAndeler = forrigeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                )
            }
        }

        return endringstidslinjePerPersonOgType.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }

    // Det regnes ikke ut som en endring dersom
    // 1. Vi har fått nye andeler som har 0 i utbetalingsbeløp
    // 2. Vi har mistet andeler som har hatt 0 i utbetalingsbeløp
    // 3. Vi har lik utbetalingsbeløp mellom nåværende og forrige andeler
    private fun lagEndringIUtbetalingForPersonOgTypeTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean, Måned> {
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val endringIBeløpTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

            nåværendeBeløp != forrigeBeløp
        }

        return endringIBeløpTidslinje
    }
    internal fun lagEtterbetalingstidslinjeForPersonOgType(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean, Måned> {
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val etterbetaling = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

            nåværendeBeløp > forrigeBeløp
        }

        return etterbetaling
    }
}
