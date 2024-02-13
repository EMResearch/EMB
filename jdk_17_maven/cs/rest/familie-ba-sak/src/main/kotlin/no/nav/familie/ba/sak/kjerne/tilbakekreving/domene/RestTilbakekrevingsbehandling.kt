package no.nav.familie.ba.sak.kjerne.tilbakekreving.domene

import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import java.time.LocalDateTime
import java.util.UUID

class RestTilbakekrevingsbehandling(
    val behandlingId: UUID,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val årsak: Behandlingsårsakstype?,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val resultat: Behandlingsresultatstype?,
    val vedtaksdato: LocalDateTime?,
)
