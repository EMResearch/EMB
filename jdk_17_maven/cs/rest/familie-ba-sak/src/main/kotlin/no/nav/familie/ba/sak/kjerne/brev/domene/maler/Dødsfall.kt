package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Dødsfall(
    override val mal: Brevmal = Brevmal.VEDTAK_OPPHØR_DØDSFALL,
    override val data: DødsfallData,
) : Brev

data class DødsfallData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val virkningstidspunkt: Flettefelt,
        val navnAvdode: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            virkningstidspunkt: String,
            navnAvdode: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            virkningstidspunkt = flettefelt(virkningstidspunkt),
            navnAvdode = flettefelt(navnAvdode),
        )
    }

    data class DelmalData(
        val signaturVedtak: SignaturVedtak,
    )
}
