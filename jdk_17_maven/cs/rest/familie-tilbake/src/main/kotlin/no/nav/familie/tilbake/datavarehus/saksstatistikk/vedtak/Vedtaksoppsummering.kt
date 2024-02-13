package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import jakarta.validation.constraints.Size
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import java.time.OffsetDateTime
import java.util.UUID

class Vedtaksoppsummering(
    @Size(min = 1, max = 20)
    val saksnummer: String,
    val ytelsestype: Ytelsestype,
    val behandlingUuid: UUID,
    val behandlingstype: Behandlingstype,
    val erBehandlingManueltOpprettet: Boolean = false,
    val behandlingOpprettetTidspunkt: OffsetDateTime,
    val vedtakFattetTidspunkt: OffsetDateTime,
    val referertFagsaksbehandling: String,
    val forrigeBehandling: UUID? = null,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String,
    val behandlendeEnhet: String,
    @Size(min = 1, max = 100)
    val perioder: List<VedtakPeriode>,
)
