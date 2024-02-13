package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class Autovedtak6og18årOgSmåbarnstillegg(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
    override val data: Autovedtak6og18årData,
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
    ) :
        this(
            data = Autovedtak6og18årData(
                delmalData = Autovedtak6og18årData.Delmaler(
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    autoUnderskrift = AutoUnderskrift(
                        enhet = vedtakFellesfelter.enhet,
                    ),
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                ),
                perioder = vedtakFellesfelter.perioder,
            ),
        )
}

data class Autovedtak6og18årData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val hjemmeltekst: Hjemmeltekst,
        val autoUnderskrift: AutoUnderskrift,
    )
}
