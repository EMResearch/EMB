import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplitt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
    personResultater: Set<PersonResultat>,
    personerIPersongrunnlag: List<Person>,
    fagsakType: FagsakType,
): List<VedtaksperiodeMedBegrunnelser> {
    val tidslinjeForSplitt = personResultater.tilTidslinjeForSplitt(personerIPersongrunnlag, fagsakType)

    val alleAndelerKombinertTidslinje = andelerTilkjentYtelse
        .tilTidslinjerPerPersonOgType().values
        .kombinerUtenNull { it }
        .filtrer { !it?.toList().isNullOrEmpty() }

    val andelerSplittetOppTidslinje =
        alleAndelerKombinertTidslinje.kombinerMed(tidslinjeForSplitt) { andelerIPeriode, splittVilkårIPeriode ->
            when (andelerIPeriode) {
                null -> null
                else -> Pair(andelerIPeriode, splittVilkårIPeriode)
            }
        }.filtrerIkkeNull()

    return andelerSplittetOppTidslinje
        .perioder()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
                tom = it.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            )
        }
}
