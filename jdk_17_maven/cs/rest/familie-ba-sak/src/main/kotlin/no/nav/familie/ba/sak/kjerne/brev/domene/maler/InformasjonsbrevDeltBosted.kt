package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevDeltBostedBrev(
    override val mal: Brevmal = Brevmal.INFORMASJONSBREV_DELT_BOSTED,
    override val data: InformasjonsbrevDeltBostedData,
) : Brev

data class InformasjonsbrevDeltBostedData(
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
