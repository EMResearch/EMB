package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

interface Vedtaksbrev : Brev {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: List<BrevPeriode>
}

enum class BrevPeriodeType(val apiNavn: String) {
    UTBETALING("utbetaling"),
    INGEN_UTBETALING("ingenUtbetaling"),
    INGEN_UTBETALING_UTEN_PERIODE("ingenUtbetalingUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget"),

    @Deprecated("Skal renames til FORTSATT_INNVILGET når det gamle implementasjonen er fjernet")
    FORTSATT_INNVILGET_NY("fortsattInnvilgetNy"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    INNVILGELSE("innvilgelse"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    INNVILGELSE_INGEN_UTBETALING("innvilgelseIngenUtbetaling"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    INNVILGELSE_KUN_UTBETALING_PÅ_SØKER("innvilgelseKunUtbetalingPaSoker"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    OPPHOR("opphor"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    AVSLAG("avslag"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    INNVILGELSE_INSTITUSJON("innvilgelseInstitusjon"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    OPPHOR_INSTITUSJON("opphorInstitusjon"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    AVSLAG_INSTITUSJON("avslagInstitusjon"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    AVSLAG_UTEN_PERIODE_INSTITUSJON("avslagUtenPeriodeInstitusjon"),

    @Deprecated("Kun UTBETALING, INGEN_UTBETALING, INGEN_UTBETALING_UTEN_PERIODE, FORTSATT_INNVILGET skal brukes")
    FORTSATT_INNVILGET_INSTITUSJON("fortsattInnvilgetInstitusjon"),
}

enum class EndretUtbetalingBrevPeriodeType(val apiNavn: String) {
    ENDRET_UTBETALINGSPERIODE("endretUtbetalingsperiode"),
    ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING("endretUtbetalingsperiodeDelvisUtbetaling"),
    ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING("endretUtbetalingsperiodeIngenUtbetaling"),
}

data class VedtakFellesfelter(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val hjemmeltekst: Hjemmeltekst,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val perioder: List<BrevPeriode>,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null,
    val korrigertVedtakData: KorrigertVedtakData? = null,
)
