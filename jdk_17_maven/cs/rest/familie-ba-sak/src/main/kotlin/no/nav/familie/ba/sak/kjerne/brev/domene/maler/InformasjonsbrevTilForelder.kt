package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevTilForelderBrev(
    override val mal: Brevmal,
    override val data: InformasjonsbrevTilForelderData,
) : Brev

data class InformasjonsbrevTilForelderData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnSoktFor: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            barnSøktFor: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnSoktFor = flettefelt(barnSøktFor),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
