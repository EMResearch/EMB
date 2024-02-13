package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode

data class HbVedtaksbrevsdata(
    @get:JsonUnwrapped val felles: HbVedtaksbrevFelles,
    val perioder: List<HbVedtaksbrevsperiode>,
) : Språkstøtte {

    @Suppress("unused") // Handlebars
    val antallPerioder = perioder.size

    override val språkkode: Språkkode = felles.språkkode
}
