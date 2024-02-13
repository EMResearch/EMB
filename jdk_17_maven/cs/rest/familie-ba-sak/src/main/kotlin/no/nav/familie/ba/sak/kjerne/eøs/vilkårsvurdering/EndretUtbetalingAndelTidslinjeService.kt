package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class EndretUtbetalingAndelTidslinjeService(
    val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
) {
    fun hentBarnasSkalIkkeUtbetalesTidslinjer(behandlingId: BehandlingId) =
        endretUtbetalingAndelHentOgPersisterService
            .hentForBehandling(behandlingId.id)
            .tilBarnasSkalIkkeUtbetalesTidslinjer()
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<Boolean, Måned>> {
    return this
        .filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ALLEREDE_UTBETALT, Årsak.ENDRE_MOTTAKER) && it.prosent == BigDecimal.ZERO }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { it.person?.aktør!! }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } } }
        .mapValues { (_, perioder) -> tidslinje { perioder } }
}

private fun <I> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> I?) = Periode(
    fraOgMed = this.fom.tilTidspunktEllerUendeligTidlig(tom),
    tilOgMed = this.tom.tilTidspunktEllerUendeligSent(fom),
    innhold = mapper(this),
)
