package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class ForsattInnvilget(
    override val mal: Brevmal,
    override val data: ForsattInnvilgetData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_FORTSATT_INNVILGET,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean = false,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        duMåMeldeFraOmEndringer: Boolean = true,
        duMåMeldeFraOmEndringerEøsSelvstendigRett: Boolean = false,
    ) :
        this(
            mal = mal,
            data = ForsattInnvilgetData(
                delmalData = ForsattInnvilgetData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    etterbetaling = etterbetaling,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                    refusjonEosAvklart = refusjonEosAvklart,
                    refusjonEosUavklart = refusjonEosUavklart,
                    duMaaMeldeFraOmEndringer = duMåMeldeFraOmEndringer,
                    duMaaMeldeFraOmEndringerEosSelvstendigRett = duMåMeldeFraOmEndringerEøsSelvstendigRett,
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

data class ForsattInnvilgetData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
        val duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean,
        val duMaaMeldeFraOmEndringer: Boolean,
    )
}
