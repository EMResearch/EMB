package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType

enum class PersonType {
    SØKER,
    ANNENPART,
    BARN,
    ;

    fun ytelseType() = when (this) {
        SØKER -> YtelseType.UTVIDET_BARNETRYGD
        BARN -> YtelseType.ORDINÆR_BARNETRYGD
        ANNENPART -> throw Feil("Finner ikke ytelsetype for annen part")
    }
}
