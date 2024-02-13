package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import java.time.LocalDateTime

data class RestTotrinnskontroll(
    val saksbehandler: String,
    val beslutter: String? = null,
    val godkjent: Boolean = false,
    val opprettetTidspunkt: LocalDateTime,
)

fun Totrinnskontroll.tilRestTotrinnskontroll() = RestTotrinnskontroll(
    saksbehandler = this.saksbehandler,
    beslutter = this.beslutter,
    godkjent = this.godkjent,
    opprettetTidspunkt = this.opprettetTidspunkt,
)
