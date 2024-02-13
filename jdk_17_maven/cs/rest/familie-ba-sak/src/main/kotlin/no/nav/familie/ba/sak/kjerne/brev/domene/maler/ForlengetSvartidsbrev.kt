package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class ForlengetSvartidsbrev(
    override val mal: Brevmal,
    override val data: ForlengetSvartidsbrevData,
) : Brev {
    constructor(
        navn: String,
        fodselsnummer: String,
        enhetNavn: String,
        mal: Brevmal,
        årsaker: List<String>,
        antallUkerSvarfrist: Int,
        organisasjonsnummer: String? = null,
        gjelder: String? = null,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data = ForlengetSvartidsbrevData(
            delmalData = ForlengetSvartidsbrevData.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhetNavn,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
            flettefelter = ForlengetSvartidsbrevData.Flettefelter(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
                antallUkerSvarfrist = flettefelt(antallUkerSvarfrist.toString()),
                aarsakerSvartidsbrev = flettefelt(årsaker),
                organisasjonsnummer = flettefelt(organisasjonsnummer),
                gjelder = flettefelt(gjelder),
            ),
        ),
    )
}

data class ForlengetSvartidsbrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val antallUkerSvarfrist: Flettefelt,
        val aarsakerSvartidsbrev: Flettefelt,
        override val organisasjonsnummer: Flettefelt,
        override val gjelder: Flettefelt,
    ) : FlettefelterForDokument

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
