package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun lagTestUtbetalingsoppdragForFGBMedToBarn(
    personIdent: String,
    fagsakId: String,
    behandlingId: Long,
    vedtakDato: LocalDate,
    datoFomBarn1: LocalDate,
    datoFomBarn2: LocalDate,
    datoTomBarn1: LocalDate,
    datoTomBarn2: LocalDate,
): Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
        Utbetalingsoppdrag.KodeEndring.NY,
        "BA",
        fagsakId,
        UUID.randomUUID().toString(),
        "SAKSBEHANDLERID",
        LocalDateTime.now(),
        listOf(
            Utbetalingsperiode(
                false,
                null,
                1,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn1,
                datoTomBarn1,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
            Utbetalingsperiode(
                false,
                null,
                2,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn2,
                datoTomBarn2,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
        ),
    )
}

fun lagTestUtbetalingsoppdragForOpphørMedToBarn(
    personIdent: String,
    fagsakId: String,
    behandlingId: Long,
    vedtakDato: LocalDate,
    datoFomBarn1: LocalDate,
    datoFomBarn2: LocalDate,
    datoTomBarn1: LocalDate,
    datoTomBarn2: LocalDate,
    opphørFom: LocalDate,
): Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
        Utbetalingsoppdrag.KodeEndring.NY,
        "BA",
        fagsakId,
        UUID.randomUUID().toString(),
        "SAKSBEHANDLERID",
        LocalDateTime.now(),
        listOf(
            Utbetalingsperiode(
                true,
                Opphør(opphørFom),
                1,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn1,
                datoTomBarn1,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
            Utbetalingsperiode(
                true,
                Opphør(opphørFom),
                2,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn2,
                datoTomBarn2,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
        ),
    )
}

fun lagTestUtbetalingsoppdragForRevurderingMedToBarn(
    personIdent: String,
    fagsakId: String,
    behandlingId: Long,
    forrigeBehandlingId: Long,
    vedtakDato: LocalDate,
    opphørFomBarn1: LocalDate,
    revurderingFomBarn1: LocalDate,
    datoFomBarn1: LocalDate,
    datoTomBarn1: LocalDate,
    opphørFomBarn2: LocalDate,
    revurderingFomBarn2: LocalDate,
    datoFomBarn2: LocalDate,
    datoTomBarn2: LocalDate,
): Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
        Utbetalingsoppdrag.KodeEndring.NY,
        "BA",
        fagsakId,
        UUID.randomUUID().toString(),
        "SAKSBEHANDLERID",
        LocalDateTime.now(),
        listOf(
            Utbetalingsperiode(
                true,
                Opphør(opphørFomBarn1),
                1,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn1,
                datoTomBarn1,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                forrigeBehandlingId,
            ),
            Utbetalingsperiode(
                false,
                null,
                3,
                1,
                vedtakDato,
                "BATR",
                revurderingFomBarn1,
                datoTomBarn1,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
            Utbetalingsperiode(
                true,
                Opphør(opphørFomBarn2),
                2,
                null,
                vedtakDato,
                "BATR",
                datoFomBarn2,
                datoTomBarn2,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                forrigeBehandlingId,
            ),
            Utbetalingsperiode(
                false,
                null,
                4,
                2,
                vedtakDato,
                "BATR",
                revurderingFomBarn2,
                datoTomBarn2,
                BigDecimal(1054),
                Utbetalingsperiode.SatsType.MND,
                personIdent,
                behandlingId,
            ),
        ),
    )
}
