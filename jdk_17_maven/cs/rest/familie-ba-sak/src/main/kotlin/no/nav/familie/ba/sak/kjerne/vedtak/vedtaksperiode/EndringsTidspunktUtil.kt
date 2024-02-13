package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFortid
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AktørOgRolleBegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VedtaksperiodeGrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VedtaksperiodeGrunnlagForPersonVilkårInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.erLikUtenFomOgTom
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import java.time.LocalDate

fun utledEndringstidspunkt(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
): LocalDate {
    val grunnlagTidslinjePerPerson =
        behandlingsGrunnlagForVedtaksperioder.copy(
            personResultater = behandlingsGrunnlagForVedtaksperioder
                .personResultater.beholdKunOppfylteVilkårResultater(),
        ).utledGrunnlagTidslinjePerPerson()
            .mapValues { it.value.vedtaksperiodeGrunnlagForPerson }
    val grunnlagTidslinjePerPersonForrigeBehandling =
        behandlingsGrunnlagForVedtaksperioderForrigeBehandling?.copy(
            personResultater = behandlingsGrunnlagForVedtaksperioderForrigeBehandling
                .personResultater.beholdKunOppfylteVilkårResultater(),
        )?.utledGrunnlagTidslinjePerPerson()
            ?.mapValues { it.value.vedtaksperiodeGrunnlagForPerson } ?: emptyMap()

    val erPeriodeLikSammePeriodeIForrigeBehandlingTidslinjer =
        grunnlagTidslinjePerPerson.outerJoin(grunnlagTidslinjePerPersonForrigeBehandling) { grunnlagForVedtaksperiode, grunnlagForVedtaksperiodeForrigeBehandling ->
            grunnlagForVedtaksperiode.erLik(grunnlagForVedtaksperiodeForrigeBehandling)
        }

    val (aktørMedFørsteEndring, datoTidligsteForskjell) =
        erPeriodeLikSammePeriodeIForrigeBehandlingTidslinjer.finnTidligsteForskjell() ?: Pair(null, TIDENES_ENDE)

    loggEndringstidspunktOgEndringer(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
        aktørMedFørsteForandring = aktørMedFørsteEndring,
        datoTidligsteForskjell = datoTidligsteForskjell,
    )

    return datoTidligsteForskjell
}

private fun Set<PersonResultat>.beholdKunOppfylteVilkårResultater(): Set<PersonResultat> = map {
    it.tilKopiForNyVilkårsvurdering(it.vilkårsvurdering)
}.toSet()

private fun loggEndringstidspunktOgEndringer(
    grunnlagTidslinjePerPerson: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
    aktørMedFørsteForandring: AktørOgRolleBegrunnelseGrunnlag?,
    datoTidligsteForskjell: LocalDate,
) {
    val grunnlagDenneBehandlingen = grunnlagTidslinjePerPerson[aktørMedFørsteForandring]
    val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørMedFørsteForandring]

    val grunnlagIPeriodeMedEndring = grunnlagDenneBehandlingen?.innholdForTidspunkt(
        datoTidligsteForskjell.toYearMonth().tilTidspunktEllerUendeligSent(),
    )?.innhold
    val grunnlagIPeriodeMedEndringForrigeBehanlding = grunnlagForrigeBehandling?.innholdForTidspunkt(
        datoTidligsteForskjell.toYearMonth().tilTidspunktEllerUendeligSent(),
    )?.innhold

    val endringer = mutableListOf<String>()

    when (grunnlagIPeriodeMedEndring) {
        is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> {
            if (grunnlagIPeriodeMedEndringForrigeBehanlding is VedtaksperiodeGrunnlagForPersonVilkårInnvilget) {
                if (!grunnlagIPeriodeMedEndring.vilkårResultaterForVedtaksperiode.erLikUtenFomOgTom(
                        grunnlagIPeriodeMedEndringForrigeBehanlding.vilkårResultaterForVedtaksperiode,
                    )
                ) {
                    endringer.add("Endring i vilkårene")
                }
                if (grunnlagIPeriodeMedEndring.kompetanse != grunnlagIPeriodeMedEndringForrigeBehanlding.kompetanse) {
                    endringer.add("Endring i kompetansen")
                }
                if (grunnlagIPeriodeMedEndring.endretUtbetalingAndel != grunnlagIPeriodeMedEndringForrigeBehanlding.endretUtbetalingAndel) {
                    endringer.add("Endring i de endrede utbetalingene")
                }
                if (grunnlagIPeriodeMedEndring.overgangsstønad != grunnlagIPeriodeMedEndringForrigeBehanlding.overgangsstønad) {
                    endringer.add("Endring i overgangsstønaden")
                }
                if (grunnlagIPeriodeMedEndring.andeler.toSet() != grunnlagIPeriodeMedEndringForrigeBehanlding.andeler.toSet()) {
                    endringer.add("Endring i andelene")
                }
            } else {
                endringer.add("Perioden var ikke innvilget i forrige behandling")
            }
        }

        is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget ->
            if (grunnlagIPeriodeMedEndringForrigeBehanlding is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget) {
                if (!grunnlagIPeriodeMedEndring.vilkårResultaterForVedtaksperiode.erLikUtenFomOgTom(
                        grunnlagIPeriodeMedEndringForrigeBehanlding.vilkårResultaterForVedtaksperiode,
                    )
                ) {
                    endringer.add("Endring i vilkårene")
                }
            } else {
                endringer.add("Perioden var innvilget i forrige behandling, men er det ikke nå lenger")
            }

        null -> endringer.add("Det er ingen vilkår på denne behandlingen i dette tidsrommet")
    }

    logger.info(
        "Endringstidspunktet for behandlingen er $datoTidligsteForskjell. Se Secure logs for å se hvem endringen er for. Dette er endringene:\n" +
            endringer.joinToString("\n"),
    )
    secureLogger.info("Ved endringstidspunktet $datoTidligsteForskjell er det endring for $aktørMedFørsteForandring")
}

private fun Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<Boolean, Måned>>.finnTidligsteForskjell() = this
    .map { (aktørOgRolleForVedtaksgrunnlag, erPeriodeLikTidslinje) ->
        val førsteEndringForAktør = erPeriodeLikTidslinje.perioder()
            .filter { it.innhold == false }
            .minOfOrNull { it.fraOgMed.tilYearMonthEllerUendeligFortid().førsteDagIInneværendeMåned() }
            ?: TIDENES_ENDE

        aktørOgRolleForVedtaksgrunnlag to førsteEndringForAktør
    }.minByOrNull { it.second }

private fun VedtaksperiodeGrunnlagForPerson?.erLik(
    grunnlagForVedtaksperiodeForrigeBehandling: VedtaksperiodeGrunnlagForPerson?,
): Boolean = when (this) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is VedtaksperiodeGrunnlagForPersonVilkårInnvilget &&
            this.vilkårResultaterForVedtaksperiode.erLikUtenFomOgTom(
                grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode,
            ) &&
            this.kompetanse == grunnlagForVedtaksperiodeForrigeBehandling.kompetanse &&
            this.endretUtbetalingAndel == grunnlagForVedtaksperiodeForrigeBehandling.endretUtbetalingAndel &&
            this.overgangsstønad == grunnlagForVedtaksperiodeForrigeBehandling.overgangsstønad &&
            this.andeler.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.andeler.toSet()

    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget &&
            this.vilkårResultaterForVedtaksperiode.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode.toSet()

    null -> grunnlagForVedtaksperiodeForrigeBehandling == null
}
