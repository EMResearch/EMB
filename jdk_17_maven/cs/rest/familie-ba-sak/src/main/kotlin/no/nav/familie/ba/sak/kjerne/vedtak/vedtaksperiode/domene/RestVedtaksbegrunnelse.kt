package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

data class RestVedtaksbegrunnelse(
    val standardbegrunnelse: String,
    val vedtakBegrunnelseSpesifikasjon: String,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
)
