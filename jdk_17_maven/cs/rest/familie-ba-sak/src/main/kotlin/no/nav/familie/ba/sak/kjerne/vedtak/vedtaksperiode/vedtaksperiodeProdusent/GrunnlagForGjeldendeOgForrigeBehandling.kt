package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

data class GrunnlagForGjeldendeOgForrigeBehandling(
    val gjeldende: VedtaksperiodeGrunnlagForPerson?,
    val erReduksjonSidenForrigeBehandling: Boolean = false,
)
