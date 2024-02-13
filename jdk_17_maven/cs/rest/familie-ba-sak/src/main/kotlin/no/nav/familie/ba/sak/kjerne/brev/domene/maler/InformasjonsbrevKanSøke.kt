package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevKanSøke(
    override val mal: Brevmal = Brevmal.INFORMASJONSBREV_KAN_SØKE,
    override val data: InformasjonsbrevKanSøkeData,
) : Brev {
    constructor(
        navn: String,
        fodselsnummer: String,
        dokumentliste: List<String>,
        enhet: String,
        saksbehandlerNavn: String,
    ) : this(
        data = InformasjonsbrevKanSøkeData(
            delmalData = InformasjonsbrevKanSøkeData.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhet,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
            flettefelter = InformasjonsbrevKanSøkeData.Flettefelter(
                navn = navn,
                fodselsnummer = fodselsnummer,
                dokumentliste = dokumentliste,
            ),
        ),
    )
}

data class InformasjonsbrevKanSøkeData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val dokumentliste: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            dokumentliste: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            dokumentliste = flettefelt(dokumentliste),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
