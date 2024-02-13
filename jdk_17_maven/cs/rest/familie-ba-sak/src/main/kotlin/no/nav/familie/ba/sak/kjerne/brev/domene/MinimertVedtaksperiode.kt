package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevPeriodeTestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import java.time.LocalDate

data class MinimertVedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BegrunnelseMedTriggere>,
    val eøsBegrunnelser: List<EØSBegrunnelseMedTriggere>,
    val fritekster: List<String> = emptyList(),
    val minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): MinimertVedtaksperiode {
    return MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        minimerteUtbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilMinimertUtbetalingsperiodeDetalj() },
        begrunnelser = this.begrunnelser.map { it.tilBegrunnelseMedTriggere(sanityBegrunnelser) },
        eøsBegrunnelser = this.eøsBegrunnelser.mapNotNull {
            it.begrunnelse.tilEØSBegrunnelseMedTriggere(
                sanityEØSBegrunnelser,
            )
        },
    )
}

fun MinimertVedtaksperiode.tilBrevPeriodeForLogging(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean = false,
    brevMålform: Målform,
    barnMedReduksjonFraForrigeBehandlingIdent: List<String> = emptyList(),
): BrevPeriodeForLogging {
    return BrevPeriodeForLogging(
        fom = this.fom,
        tom = this.tom,
        vedtaksperiodetype = this.type,
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlagForLogging() },
        fritekster = this.fritekster,
        personerPåBehandling = restBehandlingsgrunnlagForBrev.personerPåBehandling.map {
            it.tilBrevPeriodeTestPerson(
                brevPeriodeGrunnlag = this,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            )
        },
        uregistrerteBarn = uregistrerteBarn.map { it.copy(personIdent = "", navn = "") },
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        brevMålform = brevMålform,
    )
}
