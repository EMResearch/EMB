package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan

class ValutakursBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1),
) : SkjemaBuilder<Valutakurs, ValutakursBuilder>(startMåned, behandlingId) {
    fun medKurs(k: String, valutakode: String?, vararg barn: Person) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> Valutakurs.NULL
            it == '$' -> Valutakurs.NULL.copy(valutakode = valutakode)
            it?.isDigit() ?: false -> {
                Valutakurs.NULL.copy(
                    kurs = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    valutakursdato = null,
                )
            }
            else -> null
        }
    }
}
