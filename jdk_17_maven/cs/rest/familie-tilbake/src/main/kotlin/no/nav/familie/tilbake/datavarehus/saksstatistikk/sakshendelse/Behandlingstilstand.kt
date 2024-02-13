package no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse

import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Behandlingstilstand(
    val funksjoneltTidspunkt: OffsetDateTime,
    val tekniskTidspunkt: OffsetDateTime? = null,
    val saksnummer: String,
    val ytelsestype: Ytelsestype,
    val behandlingUuid: UUID,
    val referertFagsaksbehandling: String,
    val behandlingstype: Behandlingstype,
    val behandlingsstatus: Behandlingsstatus,
    val behandlingsresultat: Behandlingsresultatstype,
    val behandlingErManueltOpprettet: Boolean,
    val venterPåBruker: Boolean,
    val venterPåØkonomi: Boolean,
    val ansvarligEnhet: String,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String?,
    val forrigeBehandling: UUID?,
    val revurderingOpprettetÅrsak: Behandlingsårsakstype?,
    val totalFeilutbetaltBeløp: BigDecimal?,
    val totalFeilutbetaltPeriode: Periode?,
)
