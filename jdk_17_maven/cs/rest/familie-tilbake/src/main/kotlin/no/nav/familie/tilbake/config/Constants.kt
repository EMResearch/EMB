package no.nav.familie.tilbake.config

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period

object Constants {

    private val rettsgebyrForDato = listOf(
        Datobeløp(LocalDate.of(2021, 1, 1), 1199),
        Datobeløp(LocalDate.of(2022, 1, 1), 1223),
        Datobeløp(LocalDate.of(2023, 1, 1), 1243),
    )

    private val brukersSvarfrist: Period = Period.ofWeeks(2)

    fun brukersSvarfrist(): LocalDate = LocalDate.now().plus(brukersSvarfrist)

    fun saksbehandlersTidsfrist(): LocalDate = LocalDate.now()
        .plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)

    const val kravgrunnlagXmlRootElement: String = "urn:detaljertKravgrunnlagMelding"

    const val statusmeldingXmlRootElement: String = "urn:endringKravOgVedtakstatus"

    val rettsgebyr = rettsgebyrForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    private class Datobeløp(val gyldigFra: LocalDate, val beløp: Long)

    const val BRUKER_ID_VEDTAKSLØSNINGEN = "VL"

    val MAKS_FEILUTBETALTBELØP_PER_YTELSE =
        mapOf<Ytelsestype, BigDecimal>(
            Ytelsestype.BARNETRYGD to BigDecimal.valueOf(500),
            Ytelsestype.BARNETILSYN to BigDecimal.valueOf(rettsgebyr).multiply(BigDecimal(0.5)),
            Ytelsestype.OVERGANGSSTØNAD to BigDecimal.valueOf(rettsgebyr)
                .multiply(BigDecimal(0.5)),
            Ytelsestype.SKOLEPENGER to BigDecimal.valueOf(rettsgebyr)
                .multiply(BigDecimal(0.5)),
            Ytelsestype.KONTANTSTØTTE to BigDecimal.valueOf(rettsgebyr)
                .multiply(BigDecimal(0.5)),
        )

    const val AUTOMATISK_SAKSBEHANDLING_BEGUNNLESE = "Automatisk satt verdi"
}

object PropertyName {

    const val FAGSYSTEM = "fagsystem"
    const val ENHET = "enhet"
    const val BESLUTTER = "beslutter"
}
