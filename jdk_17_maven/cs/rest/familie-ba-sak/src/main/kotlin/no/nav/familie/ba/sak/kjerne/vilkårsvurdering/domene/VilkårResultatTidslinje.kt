package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig

class VilkårResultatTidslinje(
    private val vilkårResultater: Collection<VilkårResultat>,
) : Tidslinje<VilkårResultat, Dag>() {

    override fun lagPerioder(): List<Periode<VilkårResultat, Dag>> =
        vilkårResultater.map {
            Periode(
                fraOgMed = it.periodeFom.tilTidspunktEllerUendeligTidlig(it.periodeTom),
                tilOgMed = it.periodeTom.tilTidspunktEllerUendeligSent(it.periodeFom),
                innhold = it,
            )
        }
}

fun List<VilkårResultat>.tilTidslinje() = VilkårResultatTidslinje(this)
