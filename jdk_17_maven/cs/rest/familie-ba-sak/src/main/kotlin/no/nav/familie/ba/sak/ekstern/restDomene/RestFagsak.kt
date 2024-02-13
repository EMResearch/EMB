package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.RestTilbakekrevingsbehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime

open class RestBaseFagsak(
    open val opprettetTidspunkt: LocalDateTime,
    open val id: Long,
    open val søkerFødselsnummer: String,
    open val status: FagsakStatus,
    open val underBehandling: Boolean,
    open val løpendeKategori: BehandlingKategori?,
    open val løpendeUnderkategori: BehandlingUnderkategori?,
    open val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    open val fagsakType: FagsakType = FagsakType.NORMAL,
    open val institusjon: InstitusjonInfo? = null,
)

fun Fagsak.tilRestBaseFagsak(
    underBehandling: Boolean,
    gjeldendeUtbetalingsperioder: List<Utbetalingsperiode> = emptyList(),
    løpendeKategori: BehandlingKategori?,
    løpendeUnderkategori: BehandlingUnderkategori?,
): RestBaseFagsak = RestBaseFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.aktør.aktivFødselsnummer(),
    status = this.status,
    underBehandling = underBehandling,
    løpendeKategori = løpendeKategori,
    løpendeUnderkategori = løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
    fagsakType = this.type,
)

data class RestFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    override val løpendeKategori: BehandlingKategori?,
    override val løpendeUnderkategori: BehandlingUnderkategori?,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestUtvidetBehandling>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
    override val fagsakType: FagsakType = FagsakType.NORMAL,
) : RestBaseFagsak(
    opprettetTidspunkt = opprettetTidspunkt,
    id = id,
    søkerFødselsnummer = søkerFødselsnummer,
    status = status,
    underBehandling = underBehandling,
    løpendeKategori = løpendeKategori,
    løpendeUnderkategori = løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
    fagsakType = fagsakType,
)

fun RestBaseFagsak.tilRestFagsak(
    restUtvidetBehandlinger: List<RestUtvidetBehandling>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
) = RestFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    løpendeKategori = this.løpendeKategori,
    løpendeUnderkategori = this.løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restUtvidetBehandlinger,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
    fagsakType = this.fagsakType,
)

data class RestMinimalFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val løpendeKategori: BehandlingKategori?,
    override val løpendeUnderkategori: BehandlingUnderkategori?,
    override val underBehandling: Boolean,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestVisningBehandling>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
    val migreringsdato: LocalDate? = null,
    override val fagsakType: FagsakType,
    override val institusjon: InstitusjonInfo?,
) : RestBaseFagsak(
    opprettetTidspunkt = opprettetTidspunkt,
    id = id,
    søkerFødselsnummer = søkerFødselsnummer,
    status = status,
    underBehandling = underBehandling,
    løpendeKategori = løpendeKategori,
    løpendeUnderkategori = løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
    fagsakType = fagsakType,
    institusjon = institusjon,
)

fun RestBaseFagsak.tilRestMinimalFagsak(
    restVisningBehandlinger: List<RestVisningBehandling>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
    migreringsdato: LocalDate?,
) = RestMinimalFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    løpendeKategori = this.løpendeKategori,
    løpendeUnderkategori = this.løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restVisningBehandlinger,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
    migreringsdato = migreringsdato,
    fagsakType = this.fagsakType,
    institusjon = this.institusjon,
)
