package no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt

enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID,
}

interface Tidsenhet
class Dag : Tidsenhet
class Måned : Tidsenhet

abstract class Tidspunkt<T : Tidsenhet> internal constructor(
    internal open val uendelighet: Uendelighet,
) : Comparable<Tidspunkt<T>> {
    abstract fun flytt(tidsenheter: Long): Tidspunkt<T>
    internal abstract fun medUendelighet(uendelighet: Uendelighet): Tidspunkt<T>

    // Betrakter to uendeligheter som like, selv underliggende tidspunkt kan være forskjellig
    override fun compareTo(other: Tidspunkt<T>) =
        if (this.uendelighet == Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID) {
            0
        } else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID) {
            0
        } else if (this.uendelighet == Uendelighet.FORTID && other.uendelighet != Uendelighet.FORTID) {
            -1
        } else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet != Uendelighet.FREMTID) {
            1
        } else if (this.uendelighet != Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID) {
            1
        } else if (this.uendelighet != Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID) {
            -1
        } else {
            sammenliknMed(other)
        }

    protected abstract fun sammenliknMed(tidspunkt: Tidspunkt<T>): Int

    /**
     * Det samme som tidspunkt.somEndelig() <= tilOgMed.somEndelig()
     * Men unngår å kopiere seg selv, og trenger ikke sjekke for andre verdier av [Uendelighet] i [compareTo]
     */
    fun endeligMindreEllerLik(tidspunkt: Tidspunkt<T>) = sammenliknMed(tidspunkt) <= 0

    /**
     * Det samme som
     * tidspunkt.somEndelig() == tilOgMed.somEndelig()
     * Men unngår å kopiere seg selv, og trenger ikke sjekke for andre verdier av [Uendelighet] i [compareTo]
     */
    fun endeligLik(tidspunkt: Tidspunkt<T>) = sammenliknMed(tidspunkt) == 0

    /**
     * Det samme som tidspunkt.somEndelig() < tilOgMed.somEndelig()
     * Men unngår å kopiere seg selv, og trenger ikke sjekke for andre verdier av [Uendelighet] i [compareTo]
     */
    fun endeligMindre(tidspunkt: Tidspunkt<T>) = sammenliknMed(tidspunkt) < 0
}
