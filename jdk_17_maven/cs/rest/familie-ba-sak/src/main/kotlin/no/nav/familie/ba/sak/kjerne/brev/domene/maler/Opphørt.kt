package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class Opphørt(
    override val mal: Brevmal,
    override val data: OpphørtData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        vedtakFellesfelter: VedtakFellesfelter,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            mal = mal,
            data = OpphørtData(
                delmalData = OpphørtData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    feilutbetaling = erFeilutbetalingPåBehandling,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                    organisasjonsnummer = vedtakFellesfelter.organisasjonsnummer,
                    gjelder = vedtakFellesfelter.gjelder,
                ),
                perioder = vedtakFellesfelter.perioder,
            ),
        )
}

data class OpphørtData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val korrigertVedtak: KorrigertVedtakData?,
    )
}
