package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilUtdypendeVilkårsvurderinger
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun ISanityBegrunnelse.erGjeldendeForUtgjørendeVilkår(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    if (this.vilkår.isEmpty()) return false
    val utgjørendeVilkårResultater = finnUtgjørendeVilkår(
        begrunnelseGrunnlag = begrunnelseGrunnlag,
        sanityBegrunnelse = this,
    )

    return this.erLikVilkårOgUtdypendeVilkårIPeriode(utgjørendeVilkårResultater)
}

fun ISanityBegrunnelse.erLikVilkårOgUtdypendeVilkårIPeriode(
    vilkårResultaterForPerson: Collection<VilkårResultatForVedtaksperiode>,
): Boolean {
    return this.vilkår.all { vilkårISanityBegrunnelse ->
        val vilkårResultat = vilkårResultaterForPerson.find { it.vilkårType == vilkårISanityBegrunnelse }

        vilkårResultat != null && this.matcherMedUtdypendeVilkår(vilkårResultat)
    }
}

fun ISanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat: VilkårResultatForVedtaksperiode): Boolean {
    return when (vilkårResultat.vilkårType) {
        Vilkår.UNDER_18_ÅR -> true
        Vilkår.BOR_MED_SØKER -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.borMedSokerTriggere)
        Vilkår.GIFT_PARTNERSKAP -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.giftPartnerskapTriggere)
        Vilkår.BOSATT_I_RIKET -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.bosattIRiketTriggere)
        Vilkår.LOVLIG_OPPHOLD -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.lovligOppholdTriggere)
        // Håndteres i `erGjeldendeForSmåbarnstillegg`
        Vilkår.UTVIDET_BARNETRYGD -> UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG !in this.utvidetBarnetrygdTriggere
    }
}

private fun Collection<UtdypendeVilkårsvurdering>.erLik(
    utdypendeVilkårsvurderingFraSanityBegrunnelse: List<VilkårTrigger>?,
): Boolean {
    val utdypendeVilkårPåVilkårResultat = this.toSet()
    val utdypendeVilkårPåSanityBegrunnelse: Set<UtdypendeVilkårsvurdering> =
        utdypendeVilkårsvurderingFraSanityBegrunnelse?.tilUtdypendeVilkårsvurderinger()?.toSet() ?: emptySet()

    return utdypendeVilkårPåVilkårResultat == utdypendeVilkårPåSanityBegrunnelse
}

private fun finnUtgjørendeVilkår(
    sanityBegrunnelse: ISanityBegrunnelse,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Set<VilkårResultatForVedtaksperiode> {
    val oppfylteVilkårResultaterDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårResultaterForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?: emptyList()

    val vilkårTjent = hentVilkårResultaterTjent(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )
    val vilkårEndret = hentOppfylteVilkårResultaterEndret(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )
    val vilkårTapt = hentVilkårResultaterTapt(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        val utvidetTriggetAvInnvilgelse = hentUtvidetTriggetAvInnvilgelse(
            sanityBegrunnelse = sanityBegrunnelse,
            andelerForrigePeriode = begrunnelseGrunnlag.forrigePeriode?.andeler,
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        )
        when (sanityBegrunnelse.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING -> vilkårTjent + vilkårEndret + utvidetTriggetAvInnvilgelse
            SanityPeriodeResultat.INGEN_ENDRING -> vilkårEndret
            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> vilkårTapt + vilkårEndret

            null -> emptyList()
        }
    } else {
        vilkårTapt.takeIf {
            sanityBegrunnelse.periodeResultat in listOf(
                SanityPeriodeResultat.IKKE_INNVILGET,
                SanityPeriodeResultat.REDUKSJON,
            )
        } ?: emptyList()
    }.toSet()
}

private fun hentOppfylteVilkårResultaterEndret(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> =
    oppfylteVilkårResultaterDennePerioden.filter { vilkårResultatForrigePeriode ->
        val sammeVilkårResultatForrigePeriode =
            oppfylteVilkårResultaterForrigePeriode.singleOrNull { it.vilkårType == vilkårResultatForrigePeriode.vilkårType }

        sammeVilkårResultatForrigePeriode != null &&
            vilkårResultatForrigePeriode != sammeVilkårResultatForrigePeriode
    }

private fun hentVilkårResultaterTjent(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> {
    val innvilgedeVilkårDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }
    val innvilgedeVilkårForrigePerioden = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }

    val vilkårTjent = innvilgedeVilkårDennePerioden.toSet() - innvilgedeVilkårForrigePerioden.toSet()

    return oppfylteVilkårResultaterDennePerioden.filter { it.vilkårType in vilkårTjent }
}

private fun hentVilkårResultaterTapt(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> {
    val oppfyltDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }.toSet()
    val oppfyltForrigePeriode = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }.toSet()

    val vilkårTapt = oppfyltForrigePeriode - oppfyltDennePerioden

    return oppfylteVilkårResultaterForrigePeriode.filter { it.vilkårType in vilkårTapt }
}

private fun hentUtvidetTriggetAvInnvilgelse(
    sanityBegrunnelse: ISanityBegrunnelse,
    andelerForrigePeriode: Iterable<AndelForVedtaksperiode>?,
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> {
    if (sanityBegrunnelse.apiNavn != Standardbegrunnelse.INNVILGET_BOR_ALENE_MED_BARN.sanityApiNavn) {
        return emptyList()
    }
    val ingenAndelerForrigePeriode = andelerForrigePeriode == null || !andelerForrigePeriode.any()
    val utvidetOppfyltDennePerioden =
        oppfylteVilkårResultaterDennePerioden.filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
    return if (ingenAndelerForrigePeriode) utvidetOppfyltDennePerioden else emptyList()
}
