package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.fomErPåSatsendring
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.harBarnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate

fun Standardbegrunnelse.triggesForPeriode(
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonResultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel> = emptyList(),
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): Boolean {
    val triggesAv = sanityBegrunnelser[this]?.triggesAv ?: return false

    val aktuellePersoner = minimertePersoner
        .filter { person -> triggesAv.personTyper.contains(person.type) }
        .filter { person ->
            if (this.vedtakBegrunnelseType.erInnvilget()) {
                aktørIderMedUtbetaling.contains(person.aktørId) || person.type == PersonType.SØKER
            } else {
                true
            }
        }

    val ytelseTyperForPeriode = minimertVedtaksperiode.ytelseTyperForPeriode

    fun hentPersonerForUtgjørendeVilkår() = hentPersonerForAlleUtgjørendeVilkår(
        minimertePersonResultater = minimertePersonResultater,
        vedtaksperiode = Periode(
            fom = minimertVedtaksperiode.fom ?: TIDENES_MORGEN,
            tom = minimertVedtaksperiode.tom ?: TIDENES_ENDE,
        ),
        oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode = aktuellePersoner.map { it.tilMinimertRestPerson() },
        triggesAv = triggesAv,
        begrunnelse = this,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
    )

    return when {
        !triggesAv.valgbar -> false

        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) && !triggesAv.erEndret() -> this.vedtakBegrunnelseType.periodeErOppyltForYtelseType(
            ytelseType = if (triggesAv.småbarnstillegg) YtelseType.SMÅBARNSTILLEGG else YtelseType.UTVIDET_BARNETRYGD,
            ytelseTyperForPeriode = ytelseTyperForPeriode,
            ytelserGjeldeneForSøkerForrigeMåned = ytelserForSøkerForrigeMåned,
        ) || when {
            triggesAv.vilkår.any { it != Vilkår.UTVIDET_BARNETRYGD } -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
            else -> false
        }
        triggesAv.personerManglerOpplysninger -> minimertePersonResultater.harPersonerSomManglerOpplysninger()
        triggesAv.barnMedSeksårsdag ->
            minimertePersoner.harBarnMedSeksårsdagPåFom(minimertVedtaksperiode.fom)
        triggesAv.satsendring -> fomErPåSatsendring(minimertVedtaksperiode.fom ?: TIDENES_MORGEN)

        triggesAv.etterEndretUtbetaling ->
            erEtterEndretPeriodeAvSammeÅrsak(
                minimerteEndredeUtbetalingAndeler,
                minimertVedtaksperiode,
                aktuellePersoner,
                triggesAv,
            )

        triggesAv.erEndret() && !triggesAv.etterEndretUtbetaling -> erEndretTriggerErOppfylt(
            triggesAv = triggesAv,
            minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
            minimertVedtaksperiode = minimertVedtaksperiode,
        )
        triggesAv.gjelderFraInnvilgelsestidspunkt -> false
        triggesAv.barnDød -> dødeBarnForrigePeriode(
            ytelserForrigePeriode,
            minimertePersoner.filter { it.type === PersonType.BARN },
        ).any()
        else -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
    }
}

fun dødeBarnForrigePeriode(
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnIBehandling: List<MinimertPerson>,
): List<String> {
    return barnIBehandling.filter { barn ->
        val ytelserForrigePeriodeForBarn = ytelserForrigePeriode.filter {
            it.aktør.aktivFødselsnummer() == barn.aktivPersonIdent
        }
        var barnDødeForrigePeriode = false
        if (barn.erDød() && ytelserForrigePeriodeForBarn.isNotEmpty()) {
            val fom =
                ytelserForrigePeriodeForBarn.minOf { it.stønadFom }
            val tom =
                ytelserForrigePeriodeForBarn.maxOf { it.stønadTom }
            val fomFørDødsfall = fom <= barn.dødsfallsdato!!.toYearMonth()
            val tomEtterDødsfall = tom >= barn.dødsfallsdato.toYearMonth()
            barnDødeForrigePeriode = fomFørDødsfall && tomEtterDødsfall
        }
        barnDødeForrigePeriode
    }.map { it.aktivPersonIdent }
}

private fun erEndretTriggerErOppfylt(
    triggesAv: TriggesAv,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
): Boolean {
    val endredeAndelerSomOverlapperVedtaksperiode = minimertVedtaksperiode
        .finnEndredeAndelerISammePeriode(minimerteEndredeUtbetalingAndeler)

    return endredeAndelerSomOverlapperVedtaksperiode.any { minimertEndretAndel ->
        triggesAv.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = minimertEndretAndel,
            minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode
                .utbetalingsperioder.map { it.tilMinimertUtbetalingsperiodeDetalj() },
        )
    }
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    aktuellePersoner: List<MinimertPerson>,
    triggesAv: TriggesAv,
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
        .erDagenFør(minimertVedtaksperiode.fom) &&
        aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
        triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}

fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })

fun Standardbegrunnelse.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
            .type
            .tillatteBegrunnelsestyper
            .contains(this.vedtakBegrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.",
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        standardbegrunnelse = this,
    )
}

fun VedtakBegrunnelseType.periodeErOppyltForYtelseType(
    ytelseType: YtelseType,
    ytelseTyperForPeriode: Set<YtelseType>,
    ytelserGjeldeneForSøkerForrigeMåned: List<YtelseType>,
): Boolean {
    return when (this) {
        VedtakBegrunnelseType.INNVILGET, VedtakBegrunnelseType.INSTITUSJON_INNVILGET -> ytelseTyperForPeriode.contains(
            ytelseType,
        )

        VedtakBegrunnelseType.REDUKSJON, VedtakBegrunnelseType.INSTITUSJON_REDUKSJON -> !ytelseTyperForPeriode.contains(
            ytelseType,
        ) &&
            ytelseOppfyltForrigeMåned(ytelseType, ytelserGjeldeneForSøkerForrigeMåned)

        else -> false
    }
}

private fun ytelseOppfyltForrigeMåned(
    ytelseType: YtelseType,
    ytelserGjeldeneForSøkerForrigeMåned: List<YtelseType>,
) = ytelserGjeldeneForSøkerForrigeMåned
    .any { it == ytelseType }
