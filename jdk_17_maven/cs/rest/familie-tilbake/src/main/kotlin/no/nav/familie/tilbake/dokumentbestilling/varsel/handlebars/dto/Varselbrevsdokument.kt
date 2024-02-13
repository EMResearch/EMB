package no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate

data class Varselbrevsdokument(
    val brevmetadata: Brevmetadata,
    val beløp: Long,
    val revurderingsvedtaksdato: LocalDate,
    val feilutbetaltePerioder: List<Datoperiode>,
    val varseltekstFraSaksbehandler: String? = null,
    val fristdatoForTilbakemelding: LocalDate,
    val varsletDato: LocalDate? = null,
    val varsletBeløp: Long? = null,
    val erKorrigert: Boolean = false,
) : BaseDokument(
    brevmetadata.ytelsestype,
    brevmetadata.språkkode,
    brevmetadata.behandlendeEnhetsNavn,
    brevmetadata.ansvarligSaksbehandler,
    brevmetadata.gjelderDødsfall,
    brevmetadata.institusjon,
) {

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val harVedlegg: Boolean = brevmetadata.ytelsestype in setOf(Ytelsestype.BARNETILSYN, Ytelsestype.OVERGANGSSTØNAD)

    private val datoerHvisSammenhengendePeriode: Datoperiode? = if (feilutbetaltePerioder.size == 1) {
        Datoperiode(feilutbetaltePerioder.first().fom, feilutbetaltePerioder.first().tom)
    } else {
        null
    }

    val annenMottagersNavn: String? = BrevmottagerUtil.getAnnenMottagersNavn(brevmetadata)

    @Suppress("unused") // Handlebars
    val isYtelseMedSkatt = ytelsestype == Ytelsestype.OVERGANGSSTØNAD

    @Suppress("unused") // Handlebars
    val isRentepliktig = ytelsestype != Ytelsestype.BARNETRYGD && ytelsestype != Ytelsestype.KONTANTSTØTTE

    @Suppress("unused") // Handlebars
    val isBarnetrygd = ytelsestype == Ytelsestype.BARNETRYGD

    @Suppress("unused") // Handlebars
    val isKontantstøtte = ytelsestype == Ytelsestype.KONTANTSTØTTE

    @Suppress("unused") // Handlebars
    val isFinnesVerge
        get() = finnesVerge

    init {
        if (feilutbetaltePerioder.size == 1) {
            requireNotNull(datoerHvisSammenhengendePeriode) { "datoer for sammenhengende periode" }
        } else if (feilutbetaltePerioder.size > 1) {
            feilutbetaltePerioder.forEach {
                requireNotNull(it.fom) { "fraogmed-dato for feilutbetalingsperiode" }
                requireNotNull(it.tom) { "tilogmed-dato for feilutbetalingsperiode" }
            }
        }
        if (erKorrigert) {
            requireNotNull(varsletDato) { "varsletDato" }
            requireNotNull(varsletBeløp) { "varsletBelop" }
        }
        if (finnesVerge) {
            requireNotNull(annenMottagersNavn) { "annenMottagersNavn" }
        }
    }
}
