package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserTidslinjeTest {

    val utbetalingsperioder =
        listOf(
            VedtaksperiodeMedBegrunnelser(
                vedtak = lagVedtak(),
                type = Vedtaksperiodetype.UTBETALING,
                fom = null,
                tom = LocalDate.now(),
            ),
            VedtaksperiodeMedBegrunnelser(
                vedtak = lagVedtak(),
                type = Vedtaksperiodetype.UTBETALING,
                fom = LocalDate.now().plusDays(1),
                tom = null,
            ),
        )

    @Test
    fun `Skal returnere tidslinje hvor null-datoer er gjort om til uendelighet`() {
        val tidslinje = VedtaksperiodeMedBegrunnelserTidslinje(utbetalingsperioder)
        val perioderFraTidslinje = tidslinje.perioder()

        Assertions.assertEquals(2, perioderFraTidslinje.size)

        val periode1 = perioderFraTidslinje.first()
        val periode2 = perioderFraTidslinje.last()

        Assertions.assertEquals(DagTidspunkt.uendeligLengeSiden(), periode1.fraOgMed)
        Assertions.assertEquals(DagTidspunkt.uendeligLengeTil(), periode2.tilOgMed)
    }

    @Test
    fun `Skal gjøre om uendelighet til null-datoer`() {
        val tidslinje = VedtaksperiodeMedBegrunnelserTidslinje(utbetalingsperioder)

        val vedtaksperioderMedBegrunnelser = tidslinje.lagVedtaksperioderMedBegrunnelser()

        Assertions.assertEquals(2, vedtaksperioderMedBegrunnelser.size)

        val periode1 = vedtaksperioderMedBegrunnelser.first()
        val periode2 = vedtaksperioderMedBegrunnelser.last()

        Assertions.assertEquals(null, periode1.fom)
        Assertions.assertEquals(null, periode2.tom)
    }
}
