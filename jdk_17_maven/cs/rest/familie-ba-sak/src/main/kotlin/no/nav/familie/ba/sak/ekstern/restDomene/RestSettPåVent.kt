package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVent
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import java.time.LocalDate

data class RestSettPåVent(
    val frist: LocalDate,
    val årsak: SettPåVentÅrsak,
)

fun SettPåVent.tilRestSettPåVent(): RestSettPåVent = RestSettPåVent(
    frist = this.frist,
    årsak = this.årsak,
)
