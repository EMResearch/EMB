package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erRettFør
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somTilOgMed

fun <T : Tidsenhet, I> Iterable<Tidspunkt<T>>.tidslinjeFraTidspunkt(
    tidspunktMapper: (Tidspunkt<T>) -> Innhold<I>,
): Tidslinje<I, T> = tidslinje {
    map { tidspunkt -> TidspunktMedInnhold(tidspunkt, tidspunktMapper(tidspunkt)) }
        .filter { it.harInnhold }
        .fold(emptyList()) { perioder, tidspunktMedInnhold ->
            val sistePeriode = perioder.lastOrNull()
            when {
                sistePeriode != null && sistePeriode.kanUtvidesMed(tidspunktMedInnhold) ->
                    perioder.replaceLast(sistePeriode.utvidMed(tidspunktMedInnhold))
                else -> perioder + tidspunktMedInnhold.tilPeriode()
            }
        }
}

/**
 * Innhold har tre tilstander
 * - uten innhold               -> harInnhold = false, harVerdi = false
 * - innhold som er null        -> harInnhold = true,  harVerdi = false
 * - innhold som ikke er null   -> harInnhold = true,  harVerdi = true
 */
data class Innhold<I>(
    val innhold: I?,
    internal val harInnhold: Boolean = true,
) {
    constructor(innhold: I?) : this(innhold, true)

    companion object {
        fun <I> utenInnhold() = Innhold<I>(null, false)
    }

    val harVerdi
        get() = harInnhold && innhold != null

    val verdi
        get() = innhold!!

    fun <R> mapInnhold(mapper: (I?) -> R?): R? = if (this.harInnhold) mapper(innhold) else null
    fun <R> mapVerdi(mapper: (I) -> R): R? = if (this.harVerdi) mapper(verdi) else null
}

fun <I> I?.tilInnhold() = Innhold(this)
fun <I> I?.tilVerdi() = this?.let { Innhold<I>(it) } ?: Innhold.utenInnhold()

fun <I, T : Tidsenhet> Tidslinje<I, T>.innholdForTidspunkt(tidspunkt: Tidspunkt<T>): Innhold<I> =
    perioder().innholdForTidspunkt(tidspunkt)

fun <I, T : Tidsenhet> Collection<Periode<I, T>>.innholdForTidspunkt(
    tidspunkt: Tidspunkt<T>,
): Innhold<I> {
    val periode = this.firstOrNull { it.omfatter(tidspunkt) }
    return when (periode) {
        null -> Innhold.utenInnhold()
        else -> Innhold(periode.innhold, true)
    }
}

private fun <I, T : Tidsenhet> Periode<I, T>.omfatter(tidspunkt: Tidspunkt<T>) =
    this.fraOgMed <= tidspunkt && this.tilOgMed >= tidspunkt

private data class TidspunktMedInnhold<I, T : Tidsenhet>(
    val tidspunkt: Tidspunkt<T>,
    private val innholdsresultat: Innhold<I>,
) {
    val harInnhold get() = innholdsresultat.harInnhold
    val innhold get() = innholdsresultat.innhold
}

private fun <I, T : Tidsenhet> Periode<I, T>.kanUtvidesMed(tidspunktMedInnhold: TidspunktMedInnhold<I, T>) =
    tidspunktMedInnhold.harInnhold &&
        this.innhold == tidspunktMedInnhold.innhold &&
        this.tilOgMed.erRettFør(tidspunktMedInnhold.tidspunkt.somEndelig())

private fun <I, T : Tidsenhet> Periode<I, T>.utvidMed(tidspunktMedInnhold: TidspunktMedInnhold<I, T>): Periode<I, T> =
    this.copy(tilOgMed = tidspunktMedInnhold.tidspunkt)

private fun <I, T : Tidsenhet> TidspunktMedInnhold<I, T>.tilPeriode() =
    Periode(this.tidspunkt.somFraOgMed(), this.tidspunkt.somTilOgMed(), this.innhold)

private fun <T> Collection<T>.replaceLast(replacement: T) =
    this.take(this.size - 1) + replacement
