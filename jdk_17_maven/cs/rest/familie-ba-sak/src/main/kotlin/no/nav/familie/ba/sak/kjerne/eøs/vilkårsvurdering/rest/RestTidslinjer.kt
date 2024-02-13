package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMedEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilDag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate

fun VilkårsvurderingTidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinjer = this.søkersTidslinjer()

    val erNoenAvBarnaMellom0Og18ÅrTidslinje: Tidslinje<Boolean, Måned> = barnasTidslinjer.values
        .map { it.erUnder18ÅrVilkårTidslinje }
        .kombinerUtenNull { barnaEr0Til18ÅrListe -> barnaEr0Til18ÅrListe.any { it } }

    return RestTidslinjer(
        barnasTidslinjer = barnasTidslinjer.entries.associate {
            val erUnder18årTidslinje = it.value.erUnder18ÅrVilkårTidslinje
            it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                vilkårTidslinjer = it.value.vilkårsresultatTidslinjer.map {
                    it.beskjærEtter(erUnder18årTidslinje.tilDag())
                        .tilRestTidslinje()
                },
                oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje = it.value
                    .regelverkResultatTidslinje
                    .map { it?.kombinertResultat?.resultat }
                    .beskjærEtter(erUnder18årTidslinje)
                    .tilRestTidslinje(),
                regelverkTidslinje = it.value.regelverkResultatTidslinje
                    .map { it?.kombinertResultat?.regelverk }
                    .beskjærEtter(erUnder18årTidslinje)
                    .tilRestTidslinje(),
            )
        },
        søkersTidslinjer = RestTidslinjerForSøker(
            vilkårTidslinjer = søkersTidslinjer.vilkårsresultatTidslinjer.map {
                it.beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje.tilDag())
                    .tilRestTidslinje()
            },
            oppfyllerEgneVilkårTidslinje = søkersTidslinjer
                .regelverkResultatTidslinje.map { it?.resultat }
                .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje)
                .tilRestTidslinje(),
        ),
    )
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilRestTidslinje(): List<RestTidslinjePeriode<I>> =
    this.filtrerIkkeNull().perioder().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = periode.tilOgMed.tilSisteDagIMåneden().tilLocalDate(),
            innhold = periode.innhold!!,
        )
    }

data class RestTidslinjer(
    val barnasTidslinjer: Map<String, RestTidslinjerForBarn>,
    val søkersTidslinjer: RestTidslinjerForSøker,
)

data class RestTidslinjerForBarn(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje: List<RestTidslinjePeriode<Resultat>>,
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk>>,
)

data class RestTidslinjerForSøker(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårTidslinje: List<RestTidslinjePeriode<Resultat>>,
)

data class RestTidslinjePeriode<T>(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val innhold: T,
)
