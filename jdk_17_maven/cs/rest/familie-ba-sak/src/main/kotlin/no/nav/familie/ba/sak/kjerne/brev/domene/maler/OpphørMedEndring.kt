package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class OpphørMedEndring(
    override val mal: Brevmal,
    override val data: OpphørMedEndringData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
    ) :
        this(
            mal = mal,
            data = OpphørMedEndringData(
                delmalData = OpphørMedEndringData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    feilutbetaling = erFeilutbetalingPåBehandling,
                    etterbetaling = etterbetaling,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    refusjonEosAvklart = refusjonEosAvklart,
                    refusjonEosUavklart = refusjonEosUavklart,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    gjelder = flettefelt(vedtakFellesfelter.gjelder),
                    navn = flettefelt(vedtakFellesfelter.søkerNavn),
                    fodselsnummer = flettefelt(vedtakFellesfelter.søkerFødselsnummer),
                    organisasjonsnummer = flettefelt(vedtakFellesfelter.organisasjonsnummer),
                ),
                perioder = vedtakFellesfelter.perioder,
            ),
        )
}

data class OpphørMedEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
    )
}
