package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal

data class TriggesAv(
    val vilkår: Set<Vilkår>,
    val personTyper: Set<PersonType>,
    val personerManglerOpplysninger: Boolean,
    val satsendring: Boolean,
    val barnMedSeksårsdag: Boolean,
    val vurderingAnnetGrunnlag: Boolean,
    val medlemskap: Boolean,
    val deltbosted: Boolean,
    val deltBostedSkalIkkeDeles: Boolean,
    val valgbar: Boolean,
    val valgbarhet: Valgbarhet?,
    val endringsaarsaker: Set<Årsak>,
    val etterEndretUtbetaling: Boolean,
    val endretUtbetalingSkalUtbetales: EndretUtbetalingsperiodeDeltBostedTriggere,
    val småbarnstillegg: Boolean,
    val gjelderFørstePeriode: Boolean,
    val gjelderFraInnvilgelsestidspunkt: Boolean,
    val barnDød: Boolean,
) {
    fun erEndret() = endringsaarsaker.isNotEmpty()

    fun erUtdypendeVilkårsvurderingOppfylt(
        vilkårResultat: MinimertVilkårResultat,
    ): Boolean {
        return erDeltBostedOppfylt(vilkårResultat) &&
            erSkjønnsmessigVurderingOppfylt(vilkårResultat) &&
            erMedlemskapOppfylt(vilkårResultat) &&
            erDeltBostedSkalIkkDelesOppfylt(vilkårResultat)
    }

    fun erUtdypendeVilkårsvurderingOppfyltReduksjon(
        vilkårSomAvsluttesRettFørDennePerioden: MinimertVilkårResultat,
        vilkårSomStarterIDennePerioden: MinimertVilkårResultat?,
    ): Boolean {
        return erDeltBostedOppfyltReduksjon(
            vilkårSomAvsluttesRettFørDennePerioden = vilkårSomAvsluttesRettFørDennePerioden,
            vilkårSomStarterIDennePerioden = vilkårSomStarterIDennePerioden,
        ) &&
            erSkjønnsmessigVurderingOppfylt(vilkårSomAvsluttesRettFørDennePerioden) &&
            erMedlemskapOppfylt(vilkårSomAvsluttesRettFørDennePerioden) &&
            erDeltBostedSkalIkkDelesOppfylt(vilkårSomAvsluttesRettFørDennePerioden)
    }

    private fun erMedlemskapOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderMedlemsskap =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)

        return this.medlemskap == vilkårResultatInneholderMedlemsskap
    }

    private fun erSkjønnsmessigVurderingOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderVurderingAnnetGrunnlag =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)

        return this.vurderingAnnetGrunnlag == vilkårResultatInneholderVurderingAnnetGrunnlag
    }

    private fun erDeltBostedSkalIkkDelesOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInnholderDeltBostedSkalIkkeDeles =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES)

        return this.deltBostedSkalIkkeDeles == vilkårResultatInnholderDeltBostedSkalIkkeDeles
    }

    private fun erDeltBostedOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderDeltBosted =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

        return this.deltbosted == vilkårResultatInneholderDeltBosted
    }

    private fun erDeltBostedOppfyltReduksjon(
        vilkårSomAvsluttesRettFørDennePerioden: MinimertVilkårResultat,
        vilkårSomStarterIDennePerioden: MinimertVilkårResultat?,
    ): Boolean {
        val avsluttetVilkårInneholdtDeltBosted =
            vilkårSomAvsluttesRettFørDennePerioden.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

        val påbegyntVilkårInneholderDeltBosted = vilkårSomStarterIDennePerioden?.utdypendeVilkårsvurderinger
            ?.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) ?: false

        return if (this.deltbosted) {
            avsluttetVilkårInneholdtDeltBosted != påbegyntVilkårInneholderDeltBosted
        } else {
            !avsluttetVilkårInneholdtDeltBosted && !påbegyntVilkårInneholderDeltBosted
        }
    }
}

fun TriggesAv.erTriggereOppfyltForEndretUtbetaling(
    minimertEndretAndel: MinimertEndretAndel,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = minimertEndretAndel.oppfyllerSkalUtbetalesTrigger(this)

    val oppfyllerUtvidetScenario =
        this.endretUtbetalingSkalUtbetales == EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT ||
            endretUtbetalingBegrunnelseOppfyllerUtvidetScenario(
                vilkårBegrunnelsenGjelderFor = this.vilkår,
                minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
            )

    val erAvSammeÅrsak = this.endringsaarsaker.contains(minimertEndretAndel.årsak)

    return !hørerTilEtterEndretUtbetaling &&
        oppfyllerSkalUtbetalesTrigger &&
        oppfyllerUtvidetScenario && erAvSammeÅrsak
}

fun MinimertEndretAndel.oppfyllerSkalUtbetalesTrigger(
    triggesAv: TriggesAv,
): Boolean {
    val inneholderAndelSomSkalUtbetales = this.prosent!! != BigDecimal.ZERO
    return when (triggesAv.endretUtbetalingSkalUtbetales) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
    }
}

private fun endretUtbetalingBegrunnelseOppfyllerUtvidetScenario(
    vilkårBegrunnelsenGjelderFor: Set<Vilkår>?,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
): Boolean {
    val begrunnelseGjelderUtvidet = vilkårBegrunnelsenGjelderFor?.contains(Vilkår.UTVIDET_BARNETRYGD) ?: false

    val periodeInneholderUtvidetMedEndring = minimerteUtbetalingsperiodeDetaljer.singleOrNull {
        it.ytelseType == YtelseType.UTVIDET_BARNETRYGD
    }?.erPåvirketAvEndring == true

    return begrunnelseGjelderUtvidet == periodeInneholderUtvidetMedEndring
}
