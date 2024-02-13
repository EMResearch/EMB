package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class EnkeltInformasjonsbrev(
    override val mal: Brevmal,
    override val data: EnkeltInformasjonsbrevData,
) : Brev {

    constructor(
        navn: String,
        fodselsnummer: String,
        enhet: String,
        mal: Brevmal,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data = EnkeltInformasjonsbrevData(
            flettefelter = EnkeltInformasjonsbrevData.Flettefelter(
                navn = navn,
                fodselsnummer = fodselsnummer,
            ),
            delmalData = EnkeltInformasjonsbrevData.DelmalData(
                SignaturDelmal(
                    enhet = enhet,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
        ),
    )
}

data class EnkeltInformasjonsbrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
