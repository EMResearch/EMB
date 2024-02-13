package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype

internal object TekstformatererVedtaksbrev {

    fun lagVedtaksbrevsfritekst(vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return when (vedtaksbrevsdata.felles.vedtaksbrevstype) {
            Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT ->
                lagVedtaksbrev("vedtak/fritekstFeilutbetalingBortfalt/fritekstFeilutbetalingBortfalt", vedtaksbrevsdata)
            Vedtaksbrevstype.ORDINÆR -> lagVedtaksbrev("vedtak/vedtak", vedtaksbrevsdata)
        }
    }

    private fun lagVedtaksbrev(mal: String, vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, mal)
    }

    fun lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedlegg")
    }

    fun lagVedtaksbrevsoverskrift(vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedtak_overskrift")
    }
}
