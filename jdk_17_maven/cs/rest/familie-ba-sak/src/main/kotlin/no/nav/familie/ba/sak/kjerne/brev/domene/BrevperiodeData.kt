package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.brev.BrevPeriodeGenerator
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

@Deprecated("Skal bort. Bruk GrunnlagForBegrunnelse i stedet")
data class BrevperiodeData(
    val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val brevMålform: Målform,
    val minimertVedtaksperiode: MinimertVedtaksperiode,
    val barnMedReduksjonFraForrigeBehandlingIdent: List<String> = emptyList(),
    val minimerteKompetanserForPeriode: List<MinimertKompetanse>,
    val minimerteKompetanserSomStopperRettFørPeriode: List<MinimertKompetanse>,
    val dødeBarnForrigePeriode: List<String>,
) : Comparable<BrevperiodeData> {

    fun tilBrevPeriodeGenerator() = BrevPeriodeGenerator(
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        uregistrerteBarn = uregistrerteBarn,
        brevMålform = brevMålform,
        minimertVedtaksperiode = minimertVedtaksperiode,
        barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
        minimerteKompetanserForPeriode = minimerteKompetanserForPeriode,
        minimerteKompetanserSomStopperRettFørPeriode = minimerteKompetanserSomStopperRettFørPeriode,
        dødeBarnForrigePeriode = dødeBarnForrigePeriode,
    )

    fun hentBegrunnelserOgFritekster(): List<BrevBegrunnelse> {
        val brevPeriodeGenerator = this.tilBrevPeriodeGenerator()
        return brevPeriodeGenerator.byggBegrunnelserOgFritekster(
            begrunnelserGrunnlagMedPersoner = brevPeriodeGenerator.hentBegrunnelsegrunnlagMedPersoner(),
            eøsBegrunnelserMedKompetanser = brevPeriodeGenerator.hentEøsBegrunnelserMedKompetanser(),
        )
    }

    fun tilBrevperiodeForLogging() =
        minimertVedtaksperiode.tilBrevPeriodeForLogging(
            restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = this.uregistrerteBarn,
            brevMålform = this.brevMålform,
            barnMedReduksjonFraForrigeBehandlingIdent = this.barnMedReduksjonFraForrigeBehandlingIdent,
        )

    override fun compareTo(other: BrevperiodeData): Int {
        val fomCompared = (this.minimertVedtaksperiode.fom ?: TIDENES_MORGEN).compareTo(
            other.minimertVedtaksperiode.fom ?: TIDENES_MORGEN,
        )

        return when {
            this.erGenereltAvslag() -> 1
            other.erGenereltAvslag() -> -1
            fomCompared == 0 && this.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> 1
            fomCompared == 0 && other.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> -1
            else -> fomCompared
        }
    }

    private fun BrevperiodeData.erGenereltAvslag(): Boolean {
        return minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG &&
            minimertVedtaksperiode.fom == null &&
            minimertVedtaksperiode.tom == null
    }
}
