package no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent

import lagBrevBegrunnelse
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.lagBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erUtbetalingEllerDeltBostedIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.finnBegrunnelseGrunnlagPerPerson

fun VedtaksperiodeMedBegrunnelser.lagBrevPeriode(
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
    landkoder: Map<String, String>,
): BrevPeriode? {
    val begrunnelsesGrunnlagPerPerson = this.finnBegrunnelseGrunnlagPerPerson(grunnlagForBegrunnelse)

    val standardbegrunnelser =
        this.begrunnelser.map {
            it.standardbegrunnelse.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
            )
        }

    val eøsBegrunnelser =
        this.eøsBegrunnelser.flatMap {
            it.begrunnelse.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
                landkoder,
            )
        }

    val fritekster = this.fritekster.map { FritekstBegrunnelse(it.fritekst) }

    val begrunnelserOgFritekster =
        standardbegrunnelser + eøsBegrunnelser + fritekster

    if (begrunnelserOgFritekster.isEmpty()) return null

    return this.byggBrevPeriode(
        begrunnelserOgFritekster = begrunnelserOgFritekster,
        begrunnelseGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
        grunnlagForBegrunnelse = grunnlagForBegrunnelse,

    )
}

private fun VedtaksperiodeMedBegrunnelser.byggBrevPeriode(
    begrunnelserOgFritekster: List<BrevBegrunnelse>,
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
): BrevPeriode {
    val barnMedUtbetaling = begrunnelseGrunnlagPerPerson.finnBarnMedUtbetaling().keys
    val beløp = begrunnelseGrunnlagPerPerson.hentTotaltUtbetaltIPeriode()

    val brevPeriodeType = hentBrevPeriodeType(
        vedtaksperiodeMedBegrunnelser = this,
        erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson),
    )

    return BrevPeriode(
        fom = this.fom?.tilMånedÅr() ?: "",
        tom = hentTomTekstForBrev(brevPeriodeType),
        beløp = beløp.toString(),
        begrunnelser = begrunnelserOgFritekster,
        brevPeriodeType = brevPeriodeType,
        antallBarn = barnMedUtbetaling.size.toString(),
        barnasFodselsdager = barnMedUtbetaling.tilBarnasFødselsdatoer(),
        duEllerInstitusjonen = hentDuEllerInstitusjonenTekst(
            brevPeriodeType = brevPeriodeType,
            fagsakType = grunnlagForBegrunnelse.behandlingsGrunnlagForVedtaksperioder.fagsakType,
        ),
    )
}

private fun VedtaksperiodeMedBegrunnelser.hentTomTekstForBrev(
    brevPeriodeType: BrevPeriodeType,
) = if (this.tom == null) {
    ""
} else {
    val tomDato = this.tom.tilMånedÅr()
    when (brevPeriodeType) {
        BrevPeriodeType.UTBETALING -> "til $tomDato"
        BrevPeriodeType.INGEN_UTBETALING -> if (this.type == Vedtaksperiodetype.AVSLAG) "til og med $tomDato " else ""
        BrevPeriodeType.INGEN_UTBETALING_UTEN_PERIODE -> ""
        BrevPeriodeType.FORTSATT_INNVILGET -> ""
        BrevPeriodeType.FORTSATT_INNVILGET_NY -> ""
        else -> error("$brevPeriodeType skal ikke brukes")
    }
}

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.hentTotaltUtbetaltIPeriode() =
    this.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.finnBarnMedUtbetaling() =
    filterKeys { it.type == PersonType.BARN }
        .filterValues {
            val endretUtbetalingAndelIPeriodeErDeltBosted =
                it.dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
            val utbetalingssumIPeriode = it.dennePerioden.andeler.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }

            utbetalingssumIPeriode != 0 || endretUtbetalingAndelIPeriodeErDeltBosted
        }

fun Set<Person>.tilBarnasFødselsdatoer(): String {
    val barnasFødselsdatoerListe: List<String> = this.filter { it.type == PersonType.BARN }
        .sortedBy { it.fødselsdato }
        .map { it.fødselsdato.tilKortString() }

    return Utils.slåSammen(barnasFødselsdatoerListe)
}

private fun hentDuEllerInstitusjonenTekst(brevPeriodeType: BrevPeriodeType, fagsakType: FagsakType): String =
    when (fagsakType) {
        FagsakType.INSTITUSJON -> {
            when (brevPeriodeType) {
                BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "institusjonen"
                else -> "Institusjonen"
            }
        }

        FagsakType.NORMAL, FagsakType.BARN_ENSLIG_MINDREÅRIG -> {
            when (brevPeriodeType) {
                BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "du"
                else -> "Du"
            }
        }
    }
