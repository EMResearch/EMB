package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.testSanityKlient
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeUtilTest {

    private val sanityEØSBegrunnelser = testSanityKlient.hentEØSBegrunnelserMap()

    @Test
    fun `Skal ikke endre på utbetalingsperioder hvis det ikke finnes reduksjonsperioder`() {
        val vedtak = lagVedtak()
        val utbetalingsperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.now().minusYears(2).førsteDagIInneværendeMåned(),
                tom = LocalDate.now().minusYears(1).minusMonths(1).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.now().minusYears(1).førsteDagIInneværendeMåned(),
                tom = LocalDate.now().sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusYears(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )

        val oppdaterteUtbetalingsperioder = oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
            utbetalingsperioder = utbetalingsperioder,
            reduksjonsperioder = emptyList(),
        )

        Assertions.assertEquals(3, oppdaterteUtbetalingsperioder.size)
        Assertions.assertEquals(utbetalingsperioder, oppdaterteUtbetalingsperioder)
    }

    @Test
    fun `Skal oppdatere utbetalingsperiodene med reduksjonsperioder`() {
        val vedtak = lagVedtak()

        val utbetalingsperiode = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.now().minusYears(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusYears(1).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING,
        )
        val reduksjonsperiode = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.now().minusYears(1).plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().minusYears(1).plusMonths(6).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
        )

        val oppdaterteUtbetalingsperioder = oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
            utbetalingsperioder = listOf(utbetalingsperiode),
            reduksjonsperioder = listOf(reduksjonsperiode),
        )

        Assertions.assertEquals(3, oppdaterteUtbetalingsperioder.size)

        val periode1 = oppdaterteUtbetalingsperioder[0]
        val periode2 = oppdaterteUtbetalingsperioder[1]
        val periode3 = oppdaterteUtbetalingsperioder[2]

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode1.type)
        Assertions.assertEquals(utbetalingsperiode.fom, periode1.fom)
        Assertions.assertEquals(reduksjonsperiode.fom?.minusMonths(1)?.sisteDagIMåned(), periode1.tom)

        Assertions.assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            periode2.type,
        )
        Assertions.assertEquals(reduksjonsperiode.fom, periode2.fom)
        Assertions.assertEquals(reduksjonsperiode.tom, periode2.tom)

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode3.type)
        Assertions.assertEquals(reduksjonsperiode.tom?.plusMonths(1)?.førsteDagIInneværendeMåned(), periode3.fom)
        Assertions.assertEquals(utbetalingsperiode.tom, periode3.tom)
    }

    @Test
    fun `Skal kun lage en vedtaksperiode med type UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING hvis reduksjonsperiodene er sammenhengende og ikke krysser utbetalingsperiode-splitt`() {
        val vedtak = lagVedtak()

        val b2bFomUtbetalingsperiode = LocalDate.now().minusYears(2).førsteDagIInneværendeMåned()
        val utbetalingsperiode1 = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.now().minusYears(3).førsteDagIInneværendeMåned(),
            tom = b2bFomUtbetalingsperiode.minusMonths(1).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING,
        )
        val utbetalingsperiode2 = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = b2bFomUtbetalingsperiode,
            tom = LocalDate.now().plusYears(1).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING,
        )

        val b2bFom1 = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned()
        val b2bFom2 = LocalDate.now().førsteDagIInneværendeMåned()

        val reduksjonsperiode1 = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = LocalDate.now().minusYears(1).plusMonths(1).førsteDagIInneværendeMåned(),
            tom = b2bFom1.minusMonths(1).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
        )
        val reduksjonsperiode2 = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = b2bFom1,
            tom = b2bFom2.minusMonths(1).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
        )
        val reduksjonsperiode3 = VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = b2bFom2,
            tom = LocalDate.now().plusMonths(6).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
        )

        val oppdaterteUtbetalingsperioder = oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
            utbetalingsperioder = listOf(utbetalingsperiode1, utbetalingsperiode2),
            reduksjonsperioder = listOf(reduksjonsperiode1, reduksjonsperiode2, reduksjonsperiode3),
        )

        Assertions.assertEquals(4, oppdaterteUtbetalingsperioder.size)

        val periode1 = oppdaterteUtbetalingsperioder[0]
        val periode2 = oppdaterteUtbetalingsperioder[1]
        val periode3 = oppdaterteUtbetalingsperioder[2]
        val periode4 = oppdaterteUtbetalingsperioder[3]

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode1.type)
        Assertions.assertEquals(utbetalingsperiode1.fom, periode1.fom)
        Assertions.assertEquals(b2bFomUtbetalingsperiode.minusMonths(1)?.sisteDagIMåned(), periode1.tom)

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode2.type)
        Assertions.assertEquals(b2bFomUtbetalingsperiode, periode2.fom)
        Assertions.assertEquals(reduksjonsperiode1.fom?.minusMonths(1)?.sisteDagIMåned(), periode2.tom)

        Assertions.assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            periode3.type,
        )
        Assertions.assertEquals(reduksjonsperiode1.fom, periode3.fom)
        Assertions.assertEquals(reduksjonsperiode3.tom, periode3.tom)

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode4.type)
        Assertions.assertEquals(reduksjonsperiode3.tom?.plusMonths(1)?.førsteDagIInneværendeMåned(), periode4.fom)
        Assertions.assertEquals(utbetalingsperiode2.tom, periode4.tom)
    }

    @Test
    fun `Skal ikke slå sammen reduksjonsperioder som overlapper med utbetalingsperioder`() {
        val vedtak = lagVedtak()

        val fom1 = LocalDate.now().minusYears(1).førsteDagIInneværendeMåned()
        val fomReduksjon = fom1.plusMonths(1).førsteDagIInneværendeMåned()
        val fom2 = fomReduksjon.plusMonths(2).førsteDagIInneværendeMåned()
        val fom3 = fom2.plusMonths(3).førsteDagIInneværendeMåned()
        val sisteTom = fom3.plusMonths(5).sisteDagIMåned()

        val utbetalingsperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom1,
                tom = fom2.minusMonths(1).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom2,
                tom = fom3.minusMonths(1).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom3,
                tom = sisteTom,
                type = Vedtaksperiodetype.UTBETALING,
            ),
        )
        val reduksjonsperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fomReduksjon,
                tom = fom2.minusMonths(1).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom2,
                tom = fom3.minusMonths(1).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom3,
                tom = sisteTom,
                type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            ),
        )

        val oppdaterteUtbetalingsperioder = oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
            utbetalingsperioder = utbetalingsperioder,
            reduksjonsperioder = reduksjonsperioder,
        )

        Assertions.assertEquals(4, oppdaterteUtbetalingsperioder.size)

        val periode1 = oppdaterteUtbetalingsperioder[0]
        val periode2 = oppdaterteUtbetalingsperioder[1]
        val periode3 = oppdaterteUtbetalingsperioder[2]
        val periode4 = oppdaterteUtbetalingsperioder[3]

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, periode1.type)
        Assertions.assertEquals(fom1, periode1.fom)
        Assertions.assertEquals(fomReduksjon.minusDays(1), periode1.tom)

        Assertions.assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            periode2.type,
        )
        Assertions.assertEquals(fomReduksjon, periode2.fom)
        Assertions.assertEquals(fom2.minusDays(1), periode2.tom)

        Assertions.assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            periode3.type,
        )
        Assertions.assertEquals(fom2, periode3.fom)
        Assertions.assertEquals(fom3.minusDays(1), periode3.tom)

        Assertions.assertEquals(
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            periode4.type,
        )
        Assertions.assertEquals(fom3, periode4.fom)
        Assertions.assertEquals(sisteTom, periode4.tom)
    }
}
