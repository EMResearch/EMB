package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene

import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import java.time.LocalDate

class MinimertVedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val ytelseTyperForPeriode: Set<YtelseType>,
    val type: Vedtaksperiodetype,
    val utbetalingsperioder: List<UtbetalingsperiodeDetalj>,
) {
    fun finnEndredeAndelerISammePeriode(
        endretUtbetalingAndeler: List<MinimertEndretAndel>,
    ) = endretUtbetalingAndeler.filter {
        it.erOverlappendeMed(
            NullableMånedPeriode(
                this.fom?.toYearMonth(),
                this.tom?.toYearMonth(),
            ),
        )
    }
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(): MinimertVedtaksperiode {
    return MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        ytelseTyperForPeriode = this.utbetalingsperiodeDetaljer.map { it.ytelseType }.toSet(),
        type = this.type,
        utbetalingsperioder = this.utbetalingsperiodeDetaljer,
    )
}
