package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VarselOmRevurderingDeltBostedParagraf14Brev(
    override val mal: Brevmal = Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
    override val data: VarselOmRevurderingDeltBostedParagraf14Data,
) : Brev

data class VarselOmRevurderingDeltBostedParagraf14Data(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnMedDeltBostedAvtale: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            barnMedDeltBostedAvtale: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnMedDeltBostedAvtale = flettefelt(barnMedDeltBostedAvtale),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
