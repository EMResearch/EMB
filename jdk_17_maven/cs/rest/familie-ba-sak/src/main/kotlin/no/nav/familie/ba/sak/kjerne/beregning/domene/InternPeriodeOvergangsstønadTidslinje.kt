package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden

open class InternPeriodeOvergangsstønadTidslinje(
    private val internePeriodeOvergangsstønader: List<InternPeriodeOvergangsstønad>,
) : Tidslinje<InternPeriodeOvergangsstønad, Dag>() {

    override fun lagPerioder(): List<Periode<InternPeriodeOvergangsstønad, Dag>> {
        return internePeriodeOvergangsstønader.map {
            Periode(
                fraOgMed = it.fomDato.tilTidspunktEllerUendeligTidlig(it.tomDato),
                tilOgMed = it.tomDato.tilTidspunktEllerUendeligSent(it.fomDato),
                innhold = it,
            )
        }
    }
}

fun Tidslinje<InternPeriodeOvergangsstønad, Dag>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.perioder().mapNotNull {
        it.innhold?.copy(
            fomDato = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tomDato = it.tilOgMed.tilSisteDagIMåneden().tilLocalDate(),
        )
    }
