package no.nav.familie.ba.sak.datagenerator.brev

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

fun lagBrevBegrunnelseGrunnlagMedPersoner(
    standardbegrunnelse: Standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
    vedtakBegrunnelseType: VedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
    triggesAv: TriggesAv = lagTriggesAv(),
    personIdenter: List<String> = emptyList(),
): BrevBegrunnelseGrunnlagMedPersoner {
    return BrevBegrunnelseGrunnlagMedPersoner(
        standardbegrunnelse = standardbegrunnelse,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        triggesAv = triggesAv,
        personIdenter = personIdenter,
    )
}
