package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class Avslag(
    override val mal: Brevmal,
    override val data: AvslagData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_AVSLAG,
        vedtakFellesfelter: VedtakFellesfelter,
    ) :
        this(
            mal = mal,
            data = AvslagData(
                delmalData = AvslagData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
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

data class AvslagData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst,
        val korrigertVedtak: KorrigertVedtakData?,
    )
}
