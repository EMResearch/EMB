package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.EndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.KompetanseForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.OvergangsstønadForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.filtrerPåAktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.hentErUtbetalingSmåbarnstilleggTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilAndelerForVedtaksPeriodeTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.Companion.hentOrdinæreVilkårFor
import java.math.BigDecimal

data class BegrunnelseGrunnlagForPersonIPeriode(
    val person: Person,
    val vilkårResultater: Iterable<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) {
    fun erOrdinæreVilkårInnvilget() =
        hentOrdinæreVilkårFor(person.type).all { ordinærtVilkårForPerson ->
            vilkårResultater.any { it.vilkårType == ordinærtVilkårForPerson && it.resultat == Resultat.OPPFYLT }
        }

    fun erInnvilgetEtterEndretUtbetaling(): Boolean {
        val erEndretUtbetaling = endretUtbetalingAndel != null
        val erEndretUtbetalingPåNullProsent = endretUtbetalingAndel?.prosent == BigDecimal.ZERO
        val erÅrsakDeltBosted = endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED

        return !erEndretUtbetaling || !erEndretUtbetalingPåNullProsent || erÅrsakDeltBosted
    }

    companion object {
        fun tomPeriode(person: Person) =
            BegrunnelseGrunnlagForPersonIPeriode(person = person, vilkårResultater = emptyList(), andeler = emptyList())
    }
}

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer(): Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>> {
    return this.persongrunnlag.personer.associateWith { this.lagBegrunnelseGrunnlagForPersonTidslinje(it) }
}

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagForPersonTidslinje(person: Person): Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned> {
    val forskjøvedeVilkårResultaterForPerson =
        this.personResultater.single { it.aktør == person.aktør }
            .vilkårResultater
            .filter { it.erEksplisittAvslagPåSøknad != true }
            .tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
            .map { tidslinje -> tidslinje.map { it?.let { VilkårResultatForVedtaksperiode(it) } } }
            .kombiner { it }

    val kompetanseTidslinje = this.utfylteKompetanser.filtrerPåAktør(person.aktør)
        .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

    val endredeUtbetalingerTidslinje = this.utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
        .tilTidslinje().mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

    val andelerTilkjentYtelseTidslinje =
        this.andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje()

    val overgangsstønadTidslinje =
        this.perioderOvergangsstønad.filtrerPåAktør(person.aktør)
            .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(andelerTilkjentYtelseTidslinje.hentErUtbetalingSmåbarnstilleggTidslinje())

    return forskjøvedeVilkårResultaterForPerson
        .kombinerMed(
            andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje(),
        ) { vilkårResultater, andeler ->
            vilkårResultater?.let {
                BegrunnelseGrunnlagForPersonIPeriode(
                    person = person,
                    vilkårResultater = vilkårResultater,
                    andeler = andeler ?: emptyList(),
                )
            }
        }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
            grunnlagForPerson?.let { grunnlagForPerson.copy(kompetanse = kompetanse) }
        }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
            grunnlagForPerson?.let { grunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel) }
        }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
            grunnlagForPerson?.let { grunnlagForPerson.copy(overgangsstønad = overgangsstønad) }
        }
}
