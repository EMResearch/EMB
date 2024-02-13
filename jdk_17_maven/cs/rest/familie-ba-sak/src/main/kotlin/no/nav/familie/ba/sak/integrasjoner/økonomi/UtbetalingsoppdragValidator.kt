package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.logger
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun Utbetalingsoppdrag.validerNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) {
    if (this.utbetalingsperiode.isEmpty() && !kanHaNullutbetaling(behandlingskategori, andelerTilkjentYtelse)) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}

fun opprettAdvarselLoggVedForstattInnvilgetMedUtbetaling(
    utbetalingsoppdrag: Utbetalingsoppdrag,
    behandling: Behandling,
) {
    if (utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() &&
        behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET &&
        behandling.opprettetÅrsak != BehandlingÅrsak.ENDRE_MIGRERINGSDATO
    ) {
        logger.warn(
            "Behandling=$behandling med resultat fortsatt innvilget har utbetalingsperioder. " +
                "Dette kan tyde på at noe er galt og burde sjekkes opp.",
        )
    }
}

fun Utbetalingsoppdrag.validerOpphørsoppdrag() {
    if (this.harLøpendeUtbetaling()) {
        error("Generert utbetalingsoppdrag for opphør inneholder oppdragsperioder med løpende utbetaling.")
    }

    if (this.utbetalingsperiode.isNotEmpty() && this.utbetalingsperiode.none { it.opphør != null }) {
        error("Generert utbetalingsoppdrag for opphør mangler opphørsperioder.")
    }
}

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) = behandlingskategori == BehandlingKategori.EØS &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetaling() }
