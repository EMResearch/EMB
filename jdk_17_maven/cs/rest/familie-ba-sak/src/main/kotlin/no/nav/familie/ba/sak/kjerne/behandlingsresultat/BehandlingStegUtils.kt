package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.time.YearMonth

fun Tidslinje<Boolean, Måned>.kastFeilVedEndringEtter(
    migreringsdatoForrigeIverksatteBehandling: YearMonth,
    behandling: Behandling,
) {
    val endringIUtbetalingEtterDato = perioder()
        .filter { it.tilOgMed.tilYearMonth().isSameOrAfter(migreringsdatoForrigeIverksatteBehandling) }

    val erEndringIUtbetalingEtterMigreringsdato = endringIUtbetalingEtterDato.any { it.innhold == true }

    if (erEndringIUtbetalingEtterMigreringsdato) {
        BehandlingsresultatSteg.logger.warn("Feil i behandling $behandling.\n\nEndring i måned ${endringIUtbetalingEtterDato.first { it.innhold == true }.fraOgMed.tilYearMonth()}.")
        throw FunksjonellFeil(
            "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                "Det skal ikke skje for endre migreringsdatobehandlinger." +
                "Endringer må gjøres i en separat behandling.",
            "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                "Det skal ikke skje for endre migreringsdatobehandlinger." +
                "Endringer må gjøres i en separat behandling.",
        )
    }
}
