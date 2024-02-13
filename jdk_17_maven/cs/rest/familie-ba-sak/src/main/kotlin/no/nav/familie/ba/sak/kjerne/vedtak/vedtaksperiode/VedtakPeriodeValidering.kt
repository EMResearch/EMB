package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Håpet er at denne skal kaste feil på sikt, men enn så lenge blir det for strengt. Logger for å se behovet.
fun List<UtvidetVedtaksperiodeMedBegrunnelser>.validerPerioderInneholderBegrunnelser(
    behandlingId: Long,
    fagsakId: Long,
) {
    this.forEach {
        it.validerMinstEnBegrunnelseValgt(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnReduksjonsbegrunnelseVedReduksjon(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
            behandlingId = behandlingId,
            fagsakId = fagsakId,
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeEndretUtbetalingBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING }
    val erValgtEndretUtbetalingBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING }

    if (erMuligÅVelgeEndretUtbetalingBegrunnelse && !erValgtEndretUtbetalingBegrunnelse) {
        logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler endretubetalingsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeInnvilgetBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType.erInnvilget() }
    val erValgtInnvilgetBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType.erInnvilget() }

    if (erMuligÅVelgeInnvilgetBegrunnelse && !erValgtInnvilgetBegrunnelse) {
        logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler innvilgelsebegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnReduksjonsbegrunnelseVedReduksjon(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeReduksjonBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType.erReduksjon() }
    val erValgtReduksjonBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType.erReduksjon() }

    if (erMuligÅVelgeReduksjonBegrunnelse && !erValgtReduksjonBegrunnelse) {
        logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler reduksjonsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnBegrunnelseValgt(
    behandlingId: Long,
    fagsakId: Long,
) {
    if (this.begrunnelser.isEmpty()) {
        logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} har ingen begrunnelser knyttet til seg. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

val logger: Logger = LoggerFactory.getLogger("validerPerioderInneholderBegrunnelserLogger")
