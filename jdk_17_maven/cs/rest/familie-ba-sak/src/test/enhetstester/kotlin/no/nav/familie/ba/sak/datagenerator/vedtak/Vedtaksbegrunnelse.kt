package no.nav.familie.ba.sak.datagenerator.vedtak

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun lagVedtaksbegrunnelse(
    standardbegrunnelse: Standardbegrunnelse =
        Standardbegrunnelse.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk(),
) = Vedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    standardbegrunnelse = standardbegrunnelse,
)
