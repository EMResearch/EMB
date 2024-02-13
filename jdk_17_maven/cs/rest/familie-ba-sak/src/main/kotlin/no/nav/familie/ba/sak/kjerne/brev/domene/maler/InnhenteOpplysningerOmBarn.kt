package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerOmBarn(
    override val mal: Brevmal,
    override val data: InnhenteOpplysningerOmBarnData,
) : Brev {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        barnasFødselsdager: String,
        enhet: String,
        dokumentliste: List<String>,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data = InnhenteOpplysningerOmBarnData(
            delmalData = InnhenteOpplysningerOmBarnData.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhet,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
            flettefelter = InnhenteOpplysningerOmBarnData.Flettefelter(
                navn = navn,
                fodselsnummer = fødselsnummer,
                barnasFødselsdager = barnasFødselsdager,
                dokumentliste = dokumentliste,
            ),
        ),
    )
}

data class InnhenteOpplysningerOmBarnData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnasFodselsdatoer: Flettefelt,
        val dokumentliste: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            barnasFødselsdager: String,
            dokumentliste: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnasFodselsdatoer = flettefelt(barnasFødselsdager),
            dokumentliste = flettefelt(dokumentliste),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
