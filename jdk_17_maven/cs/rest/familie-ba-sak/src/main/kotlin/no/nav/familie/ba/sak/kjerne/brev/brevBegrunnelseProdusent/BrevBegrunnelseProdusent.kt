package no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.erAvslagUregistrerteBarnBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import java.math.BigDecimal
import java.time.LocalDate

data class GrunnlagForBegrunnelse(
    val behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    val behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    val sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    val sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    val nåDato: LocalDate,
)

fun Standardbegrunnelse.lagBrevBegrunnelse(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    grunnlag: GrunnlagForBegrunnelse,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
): BegrunnelseData {
    val sanityBegrunnelse = hentSanityBegrunnelse(grunnlag)

    val personerGjeldeneForBegrunnelse = vedtaksperiode.hentGyldigeBegrunnelserPerPerson(grunnlag)
        .mapNotNull { (person, begrunnelserPåPerson) -> person.takeIf { this in begrunnelserPåPerson } }

    val gjelderSøker = gjelderBegrunnelseSøker(personerGjeldeneForBegrunnelse)
    val barnasFødselsdatoer = sanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
        grunnlag = grunnlag,
        gjelderSøker = gjelderSøker,
        personerIBegrunnelse = personerGjeldeneForBegrunnelse,
        personerMedUtbetaling = hentPersonerMedAndelIPeriode(begrunnelsesGrunnlagPerPerson),
    )

    val antallBarn = hentAntallBarnForBegrunnelse(
        this,
        grunnlag = grunnlag,
        gjelderSøker = gjelderSøker,
        barnasFødselsdatoer = barnasFødselsdatoer,
    )

    val månedOgÅrBegrunnelsenGjelderFor = vedtaksperiode.hentMånedOgÅrForBegrunnelse()

    val grunnlagForPersonerIBegrunnelsen =
        begrunnelsesGrunnlagPerPerson.filtrerPåErPersonIBegrunnelse(personerGjeldeneForBegrunnelse)

    val beløp = hentBeløp(
        gjelderSøker = gjelderSøker,
        begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
        grunnlagForPersonerIBegrunnelsen = grunnlagForPersonerIBegrunnelsen,
    )

    val endreteUtbetalingsAndelerForBegrunnelse =
        sanityBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(grunnlagForPersonerIBegrunnelsen)

    val søknadstidspunktEndretUtbetaling = endreteUtbetalingsAndelerForBegrunnelse.sortedBy { it.søknadstidspunkt }
        .firstOrNull { sanityBegrunnelse is SanityBegrunnelse && it.årsak in sanityBegrunnelse.endringsaarsaker }?.søknadstidspunkt

    sanityBegrunnelse.validerBrevbegrunnelse(
        gjelderSøker,
        barnasFødselsdatoer,
    )

    // Kan ikke se at "kanDelesOpp" noen gang er true i gammel løsning
    if (this.kanDelesOpp) {
        throw Feil("Ingen støtte for begrunnelse som kan deles opp. Gjelder $this")
    }

    return BegrunnelseData(
        gjelderSoker = gjelderSøker,
        barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = "", // TODO Kan dette fjernes?
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = "", // TODO Kan dette fjernes?
        antallBarn = antallBarn,
        antallBarnOppfyllerTriggereOgHarUtbetaling = 0, // TODO Kan dette fjernes?
        antallBarnOppfyllerTriggereOgHarNullutbetaling = 0, // TODO Kan dette fjernes?
        maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
        maalform = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform.tilSanityFormat(),
        apiNavn = this.sanityApiNavn,
        belop = Utils.formaterBeløp(beløp),
        soknadstidspunkt = søknadstidspunktEndretUtbetaling?.tilKortString() ?: "",
        avtaletidspunktDeltBosted = "", // TODO Kan dette fjernes?
        sokersRettTilUtvidet = hentSøkersRettTilUtvidet(
            utvidetUtbetalingsdetaljer = hentUtvidetAndelerIPeriode(
                begrunnelsesGrunnlagPerPerson,
            ),
        ).tilSanityFormat(),
        vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET, // TODO kan denne fjernes?
    )
}

private fun hentUtvidetAndelerIPeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.values.flatMap { it.dennePerioden.andeler }
        .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }

fun IVedtakBegrunnelse.hentSanityBegrunnelse(grunnlag: GrunnlagForBegrunnelse) = when (this) {
    is EØSStandardbegrunnelse -> grunnlag.sanityEØSBegrunnelser[this]
    is Standardbegrunnelse -> grunnlag.sanityBegrunnelser[this]
} ?: throw Feil("Fant ikke tilsvarende sanitybegrunnelse for $this")

private fun hentPersonerMedAndelIPeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
        begrunnelseGrunnlagForPersonIPeriode.dennePerioden.andeler.toList().isNotEmpty()
    }.keys

private fun gjelderBegrunnelseSøker(personerGjeldeneForBegrunnelse: List<Person>) =
    personerGjeldeneForBegrunnelse.any { it.type == PersonType.SØKER }

fun ISanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    personerIBegrunnelse: List<Person>,
    personerMedUtbetaling: Set<Person>,
): List<LocalDate> {
    val barnPåBegrunnelse = personerIBegrunnelse.filter { it.type == PersonType.BARN }
    val barnMedUtbetaling = personerMedUtbetaling.filter { it.type == PersonType.BARN }
    val barnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.barna
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn
    return when {
        this.erAvslagUregistrerteBarnBegrunnelse() -> grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn.mapNotNull { it.fødselsdato }

        gjelderSøker && !this.gjelderEtterEndretUtbetaling && !this.gjelderEndretutbetaling -> {
            when (this.periodeResultat) {
                SanityPeriodeResultat.IKKE_INNVILGET ->
                    barnPåBehandlingen.map { it.fødselsdato } + uregistrerteBarnPåBehandlingen.mapNotNull { it.fødselsdato }

                else -> (barnMedUtbetaling + barnPåBegrunnelse).toSet().map { it.fødselsdato }
            }
        }

        else -> {
            barnPåBegrunnelse.map { it.fødselsdato }
        }
    }
}

fun hentAntallBarnForBegrunnelse(
    begrunnelse: IVedtakBegrunnelse,
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
): Int {
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn
    val erAvslagUregistrerteBarn = begrunnelse.erAvslagUregistrerteBarnBegrunnelse()

    return when {
        erAvslagUregistrerteBarn -> uregistrerteBarnPåBehandlingen.size
        gjelderSøker && begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG -> 0
        else -> barnasFødselsdatoer.size
    }
}

fun IVedtakBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this in setOf(Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN, EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN)

fun VedtaksperiodeMedBegrunnelser.hentMånedOgÅrForBegrunnelse(): String? {
    return if (this.fom == null || fom == TIDENES_MORGEN) {
        null
    } else {
        fom.forrigeMåned().tilMånedÅr()
    }
}

private fun hentBeløp(
    gjelderSøker: Boolean,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlagForPersonerIBegrunnelsen: Map<Person, IBegrunnelseGrunnlagForPeriode>,
) = if (gjelderSøker) {
    begrunnelsesGrunnlagPerPerson.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }
} else {
    grunnlagForPersonerIBegrunnelsen.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }
}

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.filtrerPåErPersonIBegrunnelse(
    personerGjeldeneForBegrunnelse: List<Person>,
) = this.filter { (k, _) -> k in personerGjeldeneForBegrunnelse }

fun ISanityBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
    grunnlagForPersonerIBegrunnelsen: Map<Person, IBegrunnelseGrunnlagForPeriode>,
) = when {
    this.gjelderEtterEndretUtbetaling -> {
        grunnlagForPersonerIBegrunnelsen.mapNotNull { it.value.forrigePeriode?.endretUtbetalingAndel }
    }

    this.gjelderEndretutbetaling -> {
        grunnlagForPersonerIBegrunnelsen.mapNotNull { it.value.dennePerioden.endretUtbetalingAndel }
    }

    else -> emptyList()
}

private fun ISanityBegrunnelse.validerBrevbegrunnelse(
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
) {
    if (!gjelderSøker && barnasFødselsdatoer.isEmpty() && !this.gjelderSatsendring && !this.erAvslagUregistrerteBarnBegrunnelse()) {
        throw IllegalStateException("Ingen personer på brevbegrunnelse")
    }
}

private fun hentSøkersRettTilUtvidet(utvidetUtbetalingsdetaljer: List<AndelForVedtaksperiode>): SøkersRettTilUtvidet {
    return when {
        utvidetUtbetalingsdetaljer.any { it.prosent > BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_FÅR_UTVIDET
        utvidetUtbetalingsdetaljer.isNotEmpty() && utvidetUtbetalingsdetaljer.all { it.prosent == BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_HAR_RETT_MEN_FÅR_IKKE

        else -> SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT
    }
}

enum class SøkersRettTilUtvidet {
    SØKER_FÅR_UTVIDET, SØKER_HAR_RETT_MEN_FÅR_IKKE, SØKER_HAR_IKKE_RETT, ;

    fun tilSanityFormat() = when (this) {
        SØKER_FÅR_UTVIDET -> "sokerFaarUtvidet"
        SØKER_HAR_RETT_MEN_FÅR_IKKE -> "sokerHarRettMenFaarIkke"
        SØKER_HAR_IKKE_RETT -> "sokerHarIkkeRett"
    }
}

fun ISanityBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this.apiNavn in setOf(
        Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN.sanityApiNavn,
        EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN.sanityApiNavn,
    )
