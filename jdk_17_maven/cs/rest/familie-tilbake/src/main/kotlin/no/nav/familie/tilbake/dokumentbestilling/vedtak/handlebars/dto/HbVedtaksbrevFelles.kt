package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.BaseDokument
import java.math.BigDecimal
import java.time.LocalDate

data class HbVedtaksbrevFelles(
    val brevmetadata: Brevmetadata,
    val søker: HbPerson,
    val fagsaksvedtaksdato: LocalDate,
    val varsel: HbVarsel? = null,
    val totalresultat: HbTotalresultat,
    val hjemmel: HbHjemmel,
    val konfigurasjon: HbKonfigurasjon,
    val fritekstoppsummering: String? = null,
    val vedtaksbrevstype: Vedtaksbrevstype,
    val ansvarligBeslutter: String? = null,
    val behandling: HbBehandling,
    val erFeilutbetaltBeløpKorrigertNed: Boolean = false,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val datoer: HbVedtaksbrevDatoer? = null,
) : BaseDokument(
    brevmetadata.ytelsestype,
    brevmetadata.språkkode,
    brevmetadata.behandlendeEnhetsNavn,
    brevmetadata.ansvarligSaksbehandler,
    brevmetadata.gjelderDødsfall,
    brevmetadata.institusjon,
) {

    @Suppress("unused") // Handlebars
    val opphørsdatoDødSøker = datoer?.opphørsdatoDødSøker

    @Suppress("unused") // Handlebars
    val opphørsdatoDødtBarn = datoer?.opphørsdatoDødtBarn

    @Suppress("unused") // Handlebars
    val opphørsdatoIkkeOmsorg = datoer?.opphørsdatoIkkeOmsorg

    val annenMottagersNavn: String? = BrevmottagerUtil.getAnnenMottagersNavn(brevmetadata)

    @Suppress("unused") // Handlebars
    val skattepliktig = Ytelsestype.OVERGANGSSTØNAD == brevmetadata.ytelsestype

    @Suppress("unused") // Handlebars
    val isSkalIkkeViseSkatt = Ytelsestype.OVERGANGSSTØNAD != brevmetadata.ytelsestype || !totalresultat.harSkattetrekk

    val harVedlegg = vedtaksbrevstype == Vedtaksbrevstype.ORDINÆR
    val hovedresultat = totalresultat.hovedresultat
}
