package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil

/**
 * Base-klassen for alle tidslinjer. Bygger på en tanke om at en tidslinje inneholder en
 * sortert liste av ikke-overlappende perioder med et innhold av type I, som kan være null.
 * Tidslinjen og tilhørende perioder har alle tidsenheten T.
 * Periodene er sortert fra tidligste til seneste.
 * fraOgMed og tilOgMed i en periode kan være like, men tilOgMed kan aldri være tidligere enn fraOgMed
 * fraOgMed i første periode kan være åpen, dvs "uenedelig lenge siden"
 * tilOgMed i siste periode kan være åpen, dvs "uendelig lenge til"
 * Generelt vil to påfølgende perioder kunne slås sammen hvis de ligger inntil hverandre
 * og innholdet er likt. Likhet avgjøres av [equals()]
 */
abstract class Tidslinje<I, T : Tidsenhet> {
    private var periodeCache: List<Periode<I, T>>? = null

    fun perioder(): Collection<Periode<I, T>> {
        return periodeCache ?: lagPerioder().sortedBy { it.fraOgMed }.toList()
            .also {
                valider(it)
                periodeCache = it
            }
    }

    protected abstract fun lagPerioder(): Collection<Periode<I, T>>

    protected open fun valider(perioder: List<Periode<I, T>>) {
        val feilInnenforPerioder = perioder.map {
            when {
                it.fraOgMed > it.tilOgMed ->
                    TidslinjeFeil(periode = it, tidslinje = this, type = TidslinjeFeilType.TOM_ER_FØR_FOM)

                else -> null
            }
        }

        val feilMellomPåfølgendePerioder = perioder.windowed(2) { (periode1, periode2) ->
            when {
                periode2.fraOgMed.erUendeligLengeSiden() ->
                    TidslinjeFeil(
                        periode = periode2,
                        tidslinje = this,
                        type = TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE,
                    )

                periode1.tilOgMed.erUendeligLengeTil() ->
                    TidslinjeFeil(
                        periode = periode1,
                        tidslinje = this,
                        type = TidslinjeFeilType.UENDELIG_FREMTID_FØR_SISTE_PERIODE,
                    )

                periode1.tilOgMed >= periode2.fraOgMed ->
                    TidslinjeFeil(
                        periode = periode1,
                        tidslinje = this,
                        type = TidslinjeFeilType.OVERLAPPER_ETTERFØLGENDE_PERIODE,
                    )

                else -> null
            }
        }

        val tidslinjeFeil = (feilInnenforPerioder + feilMellomPåfølgendePerioder)
            .filterNotNull()

        if (tidslinjeFeil.isNotEmpty()) {
            throw TidslinjeFeilException(tidslinjeFeil)
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tidslinje<*, *>) {
            perioder() == other.perioder()
        } else {
            false
        }
    }

    override fun toString(): String =
        lagPerioder().joinToString(" | ") { it.toString() }

    companion object {
        data class TidslinjeFeil(
            val type: TidslinjeFeilType,
            val periode: Periode<*, *>,
            val tidslinje: Tidslinje<*, *>,
        )

        enum class TidslinjeFeilType {
            UENDELIG_FORTID_ETTER_FØRSTE_PERIODE,
            UENDELIG_FREMTID_FØR_SISTE_PERIODE,
            TOM_ER_FØR_FOM,
            OVERLAPPER_ETTERFØLGENDE_PERIODE,
        }

        data class TidslinjeFeilException(val tidslinjeFeil: Collection<TidslinjeFeil>) :
            IllegalStateException(tidslinjeFeil.toString())
    }
}

fun <I, T : Tidsenhet> tidslinje(lagPerioder: () -> Collection<Periode<I, T>>) =
    object : Tidslinje<I, T>() {
        override fun lagPerioder() = lagPerioder()
    }

fun <I, T : Tidsenhet> List<Periode<I, T>>.tilTidslinje(): Tidslinje<I, T> = tidslinje { this }
