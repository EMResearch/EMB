package no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FlettefelterForDokument
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class VarselbrevMedÅrsakerOgBarn(
    override val mal: Brevmal,
    override val data: VarselbrevMedÅrsakerOgBarnData,
) : Brev {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        enhet: String,
        varselÅrsaker: List<String>,
        barnasFødselsdager: String,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data = VarselbrevMedÅrsakerOgBarnData(
            delmalData = VarselbrevMedÅrsakerOgBarnData.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhet,
                    saksbehandlerNavn = saksbehandlerNavn,
                ),
            ),
            flettefelter = VarselbrevMedÅrsakerOgBarnData.Flettefelter(
                navn = navn,
                fodselsnummer = fødselsnummer,
                varselÅrsaker = varselÅrsaker,
                barnasFødselsdager = barnasFødselsdager,
            ),
        ),
    )
}

data class VarselbrevMedÅrsakerOgBarnData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val varselAarsaker: Flettefelt,
        val barnasFodselsdatoer: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            varselÅrsaker: List<String>,
            barnasFødselsdager: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            varselAarsaker = flettefelt(varselÅrsaker),
            barnasFodselsdatoer = flettefelt(barnasFødselsdager),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
