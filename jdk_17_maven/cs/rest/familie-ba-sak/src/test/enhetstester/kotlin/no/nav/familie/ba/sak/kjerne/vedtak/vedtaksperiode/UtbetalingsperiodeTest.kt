package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagUtbetalingsperiode
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingsperiodeTest {

    val søker = tilfeldigSøker()
    val fomDato1 = LocalDate.now().minusMonths(2).withDayOfMonth(1)
    val fomDato2 = LocalDate.now().minusMonths(1).withDayOfMonth(1)
    val fomDato3 = LocalDate.now().withDayOfMonth(1)
    val utbetalingsperiode1 = lagUtbetalingsperiode(
        periodeFom = fomDato1,
        periodeTom = fomDato1.let { it.withDayOfMonth(it.lengthOfMonth()) },
        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
    )
    val utbetalingsperiode2 = lagUtbetalingsperiode(
        periodeFom = fomDato2,
        periodeTom = fomDato2.let { it.withDayOfMonth(it.lengthOfMonth()) },
        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
    )
    val utbetalingsperiode3 = lagUtbetalingsperiode(
        periodeFom = fomDato3,
        periodeTom = fomDato3.let { it.withDayOfMonth(it.lengthOfMonth()) },
        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
    )
    val utbetalingsperioder = listOf(
        utbetalingsperiode1,
        utbetalingsperiode2,
        utbetalingsperiode3,
    )

    @Test
    fun `Skal gi riktig siste utbetalingsperiode som er tidligere eller samme måned som inneværende måned`() {
        val utbetalingsperiodeForVedtaksperiode =
            hentUtbetalingsperiodeForVedtaksperiode(
                utbetalingsperioder,
                fomDato2,
            )

        Assertions.assertEquals(utbetalingsperiode2.periodeFom, utbetalingsperiodeForVedtaksperiode.periodeFom)
        Assertions.assertEquals(utbetalingsperiode2.periodeTom, utbetalingsperiodeForVedtaksperiode.periodeTom)
        Assertions.assertEquals(utbetalingsperiode2.vedtaksperiodetype, utbetalingsperiodeForVedtaksperiode.vedtaksperiodetype)
        Assertions.assertEquals(
            utbetalingsperiode2.utbetalingsperiodeDetaljer,
            utbetalingsperiodeForVedtaksperiode.utbetalingsperiodeDetaljer,
        )
        Assertions.assertEquals(utbetalingsperiode2.ytelseTyper, utbetalingsperiodeForVedtaksperiode.ytelseTyper)
        Assertions.assertEquals(utbetalingsperiode2.antallBarn, utbetalingsperiodeForVedtaksperiode.antallBarn)
        Assertions.assertEquals(utbetalingsperiode2.utbetaltPerMnd, utbetalingsperiodeForVedtaksperiode.utbetaltPerMnd)
    }

    @Test
    fun `Skal gi utbetalingsperiode i inneværende måned dersom fom er null`() {
        val utbetalingsperiodeForVedtaksperiode =
            hentUtbetalingsperiodeForVedtaksperiode(
                utbetalingsperioder,
                null,
            )

        Assertions.assertEquals(utbetalingsperiode3.periodeFom, utbetalingsperiodeForVedtaksperiode.periodeFom)
        Assertions.assertEquals(utbetalingsperiode3.periodeTom, utbetalingsperiodeForVedtaksperiode.periodeTom)
        Assertions.assertEquals(utbetalingsperiode3.vedtaksperiodetype, utbetalingsperiodeForVedtaksperiode.vedtaksperiodetype)
        Assertions.assertEquals(
            utbetalingsperiode3.utbetalingsperiodeDetaljer,
            utbetalingsperiodeForVedtaksperiode.utbetalingsperiodeDetaljer,
        )
        Assertions.assertEquals(utbetalingsperiode3.ytelseTyper, utbetalingsperiodeForVedtaksperiode.ytelseTyper)
        Assertions.assertEquals(utbetalingsperiode3.antallBarn, utbetalingsperiodeForVedtaksperiode.antallBarn)
        Assertions.assertEquals(utbetalingsperiode3.utbetaltPerMnd, utbetalingsperiodeForVedtaksperiode.utbetaltPerMnd)
    }
}
