package no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse

data class BrevPeriode(
    val fom: Flettefelt,
    val tom: Flettefelt,
    val belop: Flettefelt,
    val antallBarn: Flettefelt,
    val barnasFodselsdager: Flettefelt,
    val begrunnelser: List<BrevBegrunnelse>,
    val type: Flettefelt,
    val duEllerInstitusjonen: Flettefelt,
) {

    constructor(
        fom: String,
        tom: String,
        beløp: String,
        begrunnelser: List<BrevBegrunnelse>,
        brevPeriodeType: BrevPeriodeType,
        antallBarn: String,
        barnasFodselsdager: String,
        duEllerInstitusjonen: String,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(tom),
        belop = flettefelt(beløp),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        begrunnelser = begrunnelser,
        type = flettefelt(brevPeriodeType.apiNavn),
        duEllerInstitusjonen = flettefelt(duEllerInstitusjonen),
    )
}
