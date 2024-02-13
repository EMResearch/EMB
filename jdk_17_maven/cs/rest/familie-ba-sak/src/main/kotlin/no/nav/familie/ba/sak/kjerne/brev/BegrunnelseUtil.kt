package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.domene.somOverlapper
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentPersonerForEtterEndretUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.barnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun hentPersonidenterGjeldendeForBegrunnelse(
    triggesAv: TriggesAv,
    begrunnelse: IVedtakBegrunnelse,
    periode: NullablePeriode,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    vedtaksperiodetype: Vedtaksperiodetype,
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    identerMedUtbetalingPåPeriode: List<String>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    identerMedReduksjonPåPeriode: List<String> = emptyList(),
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
    dødeBarnForrigePeriode: List<String>,
): Set<String> {
    val erFortsattInnvilgetBegrunnelse = vedtakBegrunnelseType.erFortsattInnvilget()
    val erEndretUtbetalingBegrunnelse = vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING
    val erUtbetalingMedReduksjonFraSistIverksatteBehandling =
        vedtaksperiodetype == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING && vedtakBegrunnelseType.erReduksjon() && !triggesAv.vilkår.contains(
            Vilkår.UNDER_18_ÅR,
        )

    fun hentPersonerForUtgjørendeVilkår() = hentPersonerForAlleUtgjørendeVilkår(
        minimertePersonResultater = restBehandlingsgrunnlagForBrev.minimertePersonResultater,
        vedtaksperiode = Periode(
            fom = periode.fom ?: TIDENES_MORGEN,
            tom = periode.tom ?: TIDENES_ENDE,
        ),
        oppdatertBegrunnelseType = vedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode = hentAktuellePersonerForVedtaksperiode(
            restBehandlingsgrunnlagForBrev.personerPåBehandling,
            vedtakBegrunnelseType,
            identerMedUtbetalingPåPeriode,
        ),
        begrunnelse = begrunnelse,
        triggesAv = triggesAv,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
    ).map { person -> person.personIdent }

    return when {
        (triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) || triggesAv.småbarnstillegg) && !erEndretUtbetalingBegrunnelse -> hentPersonerForUtvidetOgSmåbarnstilleggBegrunnelse(
            identerMedUtbetaling = identerMedUtbetalingPåPeriode,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            periode = periode,
            fagsakType = restBehandlingsgrunnlagForBrev.fagsakType,
        ) + when {
            triggesAv.vilkår.any { it != Vilkår.UTVIDET_BARNETRYGD } -> hentPersonerForUtgjørendeVilkår()
            else -> emptyList()
        }

        triggesAv.barnMedSeksårsdag -> restBehandlingsgrunnlagForBrev.personerPåBehandling.barnMedSeksårsdagPåFom(
            periode.fom,
        ).map { person -> person.personIdent }

        triggesAv.personerManglerOpplysninger -> if (restBehandlingsgrunnlagForBrev.minimertePersonResultater.harPersonerSomManglerOpplysninger()) {
            emptyList()
        } else {
            error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")
        }

        erFortsattInnvilgetBegrunnelse -> identerMedUtbetalingPåPeriode
        erEndretUtbetalingBegrunnelse -> hentPersonerForEndretUtbetalingBegrunnelse(
            triggesAv = triggesAv,
            endredeUtbetalingAndelerSomOverlapperMedPeriode = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler.filter {
                it.erOverlappendeMed(
                    nullableMånedPeriode = periode.tilNullableMånedPeriode(),
                )
            },
            minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
        )

        erUtbetalingMedReduksjonFraSistIverksatteBehandling -> identerMedReduksjonPåPeriode

        triggesAv.etterEndretUtbetaling -> hentPersonerForEtterEndretUtbetalingsperiode(
            minimerteEndredeUtbetalingAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler,
            fom = periode.fom,
            endringsaarsaker = triggesAv.endringsaarsaker,
        )

        triggesAv.barnDød -> dødeBarnForrigePeriode

        else -> hentPersonerForUtgjørendeVilkår()
    }.toSet()
}

private fun hentPersonerForEndretUtbetalingBegrunnelse(
    triggesAv: TriggesAv,
    endredeUtbetalingAndelerSomOverlapperMedPeriode: List<MinimertRestEndretAndel>,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
): List<String> {
    val personerMedRiktigTypeEndringer =
        endredeUtbetalingAndelerSomOverlapperMedPeriode.filter { triggesAv.endringsaarsaker.contains(it.årsak) }
            .map { it.personIdent }
    return minimerteUtbetalingsperiodeDetaljer.filter { it.erPåvirketAvEndring }.filter {
        when (triggesAv.endretUtbetalingSkalUtbetales) {
            EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
            EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> {
                it.utbetaltPerMnd > 0
            }

            EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> it.utbetaltPerMnd == 0
        }
    }.map { it.person.personIdent }.filter { personerMedRiktigTypeEndringer.contains(it) }
}

/**
 * Selv om utvidet kun gjelder for søker ønsker vi å si noe om hvilke barn søker får utvidet for.
 * Dette vil være alle barn med utbetaling og alle barn med endret utbetaling i samme periode.
 *
 * For eksempel om søker oppfyller vilkårene til delt bosted og utvidet barnetrygd, men barnetrygden allerede er
 * sendt ut til partner, og delt bosted er endret til at det ikke er noen utbetaling, ønsker vi fremdeles å ta med
 * barna uten utbetaling i begrunnelsen.
 *
 * Søker må med selv om det ikke er utbetaling på søker slik at det blir riktig ved avslag.
 */
private fun hentPersonerForUtvidetOgSmåbarnstilleggBegrunnelse(
    identerMedUtbetaling: List<String>,
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    periode: NullablePeriode,
    fagsakType: FagsakType,
): List<String> {
    val identerFraSammenfallendeEndringsperioder =
        restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler.somOverlapper(periode.tilNullableMånedPeriode())
            .map { it.personIdent }

    val søkersIdent =
        restBehandlingsgrunnlagForBrev.personerPåBehandling.find {
            when (fagsakType) {
                FagsakType.NORMAL,
                FagsakType.INSTITUSJON,
                -> it.type == PersonType.SØKER

                FagsakType.BARN_ENSLIG_MINDREÅRIG -> it.type == PersonType.BARN
            }
        }?.personIdent
            ?: throw IllegalStateException("Søker mangler i behandlingsgrunnlag for brev")

    return identerMedUtbetaling + identerFraSammenfallendeEndringsperioder + søkersIdent
}

private fun hentAktuellePersonerForVedtaksperiode(
    personerPåBehandling: List<MinimertRestPerson>,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    identerMedUtbetalingPåPeriode: List<String>,
): List<MinimertRestPerson> = personerPåBehandling.filter { person ->
    if (vedtakBegrunnelseType.erInnvilget()) {
        identerMedUtbetalingPåPeriode.contains(person.personIdent) || person.type == PersonType.SØKER
    } else {
        true
    }
}
