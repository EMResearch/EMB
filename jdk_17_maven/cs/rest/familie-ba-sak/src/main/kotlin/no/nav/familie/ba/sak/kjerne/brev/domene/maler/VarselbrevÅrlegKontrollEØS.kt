package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VarselbrevÅrlegKontrollEøs(
    override val mal: Brevmal,
    override val data: VarselbrevÅrlegKontrollEøsData,
) : Brev {

    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        enhet: String,
        mottakerlandSed: String,
        dokumentliste: List<String> = emptyList(),
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data = VarselbrevÅrlegKontrollEøsData(
            delmalData = VarselbrevÅrlegKontrollEøsData.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhet,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
            flettefelter = VarselbrevÅrlegKontrollEøsData.Flettefelter(
                navn = navn,
                fodselsnummer = fødselsnummer,
                mottakerlandSed = mottakerlandSed,
                dokumentliste = dokumentliste,
            ),
        ),
    )
}

data class VarselbrevÅrlegKontrollEøsData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val mottakerlandSed: Flettefelt,
        val dokumentliste: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            mottakerlandSed: String,
            dokumentliste: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            mottakerlandSed = flettefelt(mottakerlandSed),
            dokumentliste = flettefelt(dokumentliste),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
