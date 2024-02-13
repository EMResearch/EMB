package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val eksternBrukId: UUID,
    val behandlingId: UUID,
    val erBehandlingHenlagt: Boolean,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val opprettetDato: LocalDate,
    val avsluttetDato: LocalDate? = null,
    val endretTidspunkt: LocalDateTime,
    val vedtaksdato: LocalDate? = null,
    val enhetskode: String,
    val enhetsnavn: String,
    val resultatstype: Behandlingsresultatstype? = null,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String? = null,
    val erBehandlingPåVent: Boolean,
    val kanHenleggeBehandling: Boolean,
    val kanRevurderingOpprettes: Boolean = false,
    val harVerge: Boolean,
    val kanEndres: Boolean,
    val varselSendt: Boolean,
    val behandlingsstegsinfo: List<BehandlingsstegsinfoDto>,
    val fagsystemsbehandlingId: String,
    val eksternFagsakId: String,
    val behandlingsårsakstype: Behandlingsårsakstype? = null,
    val støtterManuelleBrevmottakere: Boolean,
    val harManuelleBrevmottakere: Boolean,
    val manuelleBrevmottakere: List<ManuellBrevmottakerResponsDto>,
)

data class BehandlingsstegsinfoDto(
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegstatus: Behandlingsstegstatus,
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
)
