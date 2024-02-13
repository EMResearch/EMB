package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun sats(ytelseType: YtelseType) =
    when (ytelseType) {
        YtelseType.ORDINÆR_BARNETRYGD -> 1054
        YtelseType.UTVIDET_BARNETRYGD -> 1054
        YtelseType.SMÅBARNSTILLEGG -> 660
    }

fun lagUtbetalingsoppdrag(utbetalingsperiode: List<Utbetalingsperiode>) = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = "BA",
    saksnummer = "",
    aktoer = UUID.randomUUID().toString(),
    saksbehandlerId = "",
    avstemmingTidspunkt = LocalDateTime.now(),
    utbetalingsperiode = utbetalingsperiode,
)

fun lagUtbetalingsoppdrag(
    utbetalingsperiode: List<no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode> = listOf(
        lagUtbetalingsperiode(),
    ),
) =
    no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag(
        kodeEndring = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "BA",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperiode,
    )

fun lagUtbetalingsperiode() = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode(
    erEndringPåEksisterendePeriode = false,
    opphør = null,
    periodeId = 0,
    forrigePeriodeId = null,
    datoForVedtak = LocalDate.now(),
    klassifisering = "",
    vedtakdatoFom = LocalDate.now(),
    vedtakdatoTom = LocalDate.now().plusMonths(1),
    sats = BigDecimal(100),
    satsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
    utbetalesTil = "",
    behandlingId = 1,
    utbetalingsgrad = 100,
)
