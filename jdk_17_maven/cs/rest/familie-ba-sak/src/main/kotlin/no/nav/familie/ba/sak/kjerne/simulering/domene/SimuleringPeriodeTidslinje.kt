package no.nav.familie.ba.sak.kjerne.simulering.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt

class SimuleringPeriodeTidslinje(
    val simuleringsPerioder: Collection<SimuleringsPeriode>,
) : Tidslinje<SimuleringsPeriode, Måned>() {
    override fun lagPerioder(): Collection<Periode<SimuleringsPeriode, Måned>> =
        simuleringsPerioder.map {
            Periode(
                fraOgMed = it.fom.tilMånedTidspunkt(),
                tilOgMed = it.tom.tilMånedTidspunkt(),
                innhold = it,
            )
        }
}

fun List<SimuleringsPeriode>.tilTidslinje(): Tidslinje<SimuleringsPeriode, Måned> = SimuleringPeriodeTidslinje(this)
