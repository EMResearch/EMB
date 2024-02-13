package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime

data class RestUtvidetBehandling(
    val behandlingId: Long,
    val steg: StegType,
    val stegTilstand: List<RestBehandlingStegTilstand>,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val skalBehandlesAutomatisk: Boolean,
    val type: BehandlingType,
    val kategori: BehandlingKategori,
    val underkategori: BehandlingUnderkategoriDTO,
    val årsak: BehandlingÅrsak,
    val opprettetTidspunkt: LocalDateTime,
    val endretAv: String,
    val arbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
    val søknadsgrunnlag: SøknadDTO?,
    val personer: List<RestPerson>,
    val personResultater: List<RestPersonResultat>,
    val fødselshendelsefiltreringResultater: List<RestFødselshendelsefiltreringResultat>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndeler>,
    val endretUtbetalingAndeler: List<RestEndretUtbetalingAndel>,
    val kompetanser: List<RestKompetanse>,
    val tilbakekreving: RestTilbakekreving?,
    val vedtak: RestVedtak?,
    val totrinnskontroll: RestTotrinnskontroll?,
    val aktivSettPåVent: RestSettPåVent?,
    val migreringsdato: LocalDate?,
    val valutakurser: List<RestValutakurs>,
    val utenlandskePeriodebeløp: List<RestUtenlandskPeriodebeløp>,
    val verge: VergeInfo?,
    val korrigertEtterbetaling: RestKorrigertEtterbetaling?,
    val korrigertVedtak: RestKorrigertVedtak?,
    val feilutbetaltValuta: List<RestFeilutbetaltValuta>,
    val brevmottakere: List<RestBrevmottaker>,
    val refusjonEøs: List<RestRefusjonEøs>,
)
