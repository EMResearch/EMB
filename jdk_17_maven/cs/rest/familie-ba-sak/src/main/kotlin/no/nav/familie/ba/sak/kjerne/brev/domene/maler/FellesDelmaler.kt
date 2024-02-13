package no.nav.familie.ba.sak.kjerne.brev.domene.maler

data class SignaturDelmal(
    val enhet: Flettefelt,
    val saksbehandler: Flettefelt,
) {

    constructor(enhet: String, saksbehandlerNavn: String) : this(
        enhet = flettefelt(enhet),
        saksbehandler = flettefelt(saksbehandlerNavn),
    )
}

data class SignaturVedtak(
    val enhet: Flettefelt,
    val saksbehandler: Flettefelt,
    val beslutter: Flettefelt,
) {

    constructor(enhet: String, saksbehandler: String, beslutter: String) : this(
        flettefelt(enhet),
        flettefelt(saksbehandler),
        flettefelt(beslutter),
    )
}

data class Etterbetaling(
    val etterbetalingsbelop: Flettefelt,
) {

    constructor(etterbetalingsbeløp: String) : this(
        flettefelt(etterbetalingsbeløp),
    )
}

data class EtterbetalingInstitusjon(
    val etterbetalingsbelop: Flettefelt,
) {

    constructor(etterbetalingsbeløp: String) : this(
        flettefelt(etterbetalingsbeløp),
    )
}

data class Hjemmeltekst(
    val hjemler: Flettefelt,
) {

    constructor(hjemler: String) : this(
        flettefelt(hjemler),
    )
}

data class AutoUnderskrift(
    val enhet: Flettefelt,
) {

    constructor(enhet: String) : this(
        flettefelt(enhet),
    )
}

data class KorrigertVedtakData(
    val datoKorrigertVedtak: Flettefelt,
) {

    constructor(datoKorrigertVedtak: String) : this(
        flettefelt(datoKorrigertVedtak),
    )
}

data class FeilutbetaltValuta(
    val perioderMedForMyeUtbetalt: Flettefelt,
) {

    constructor(perioderMedForMyeUtbetalt: Set<String>) : this(
        flettefelt(perioderMedForMyeUtbetalt.toList()),
    )
}

data class RefusjonEøsAvklart(
    val perioderMedRefusjonEosAvklart: Flettefelt,
) {
    constructor(perioderMedRefusjonEøsAvklart: Set<String>) : this(
        flettefelt(perioderMedRefusjonEøsAvklart.toList()),
    )
}

data class RefusjonEøsUavklart(
    val perioderMedRefusjonEosUavklart: Flettefelt,
) {
    constructor(perioderMedRefusjonEøsUavklart: Set<String>) : this(
        flettefelt(perioderMedRefusjonEøsUavklart.toList()),
    )
}
