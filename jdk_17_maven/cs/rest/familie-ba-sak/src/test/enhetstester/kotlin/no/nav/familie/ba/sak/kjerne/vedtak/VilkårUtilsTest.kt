package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.sorter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårUtilsTest {

    /**
     * Korrekt rekkefølge:
     * 1. Utbetalings-, opphørs- og avslagsperioder sortert på fom-dato
     * 2. Avslagsperioder uten datoer
     */
    @Test
    fun `vedtaksperioder sorteres korrekt til brev`() {
        val avslagMedTomDatoInneværendeMåned = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().minusMonths(6),
            tom = LocalDate.now(),
            type = Vedtaksperiodetype.AVSLAG,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),

        )
        val avslagUtenTomDato =
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().minusMonths(5),
                tom = null,
                type = Vedtaksperiodetype.AVSLAG,
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            )
        val opphørsperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().minusMonths(4),
            tom = LocalDate.now().minusMonths(1),
            type = Vedtaksperiodetype.OPPHØR,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val utbetalingsperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().minusMonths(3),
            tom = LocalDate.now().minusMonths(1),
            type = Vedtaksperiodetype.UTBETALING,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val avslagUtenDatoer = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = null,
            tom = null,
            type = Vedtaksperiodetype.AVSLAG,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val sorterteVedtaksperioder =
            listOf(
                utbetalingsperiode,
                opphørsperiode,
                avslagMedTomDatoInneværendeMåned,
                avslagUtenDatoer,
                avslagUtenTomDato,
            ).shuffled().sorter()

        // Utbetalingsperiode, opphørspersiode og avslagsperiode med fom-dato sorteres kronologisk
        Assertions.assertEquals(avslagMedTomDatoInneværendeMåned, sorterteVedtaksperioder[0])
        Assertions.assertEquals(avslagUtenTomDato, sorterteVedtaksperioder[1])
        Assertions.assertEquals(opphørsperiode, sorterteVedtaksperioder[2])
        Assertions.assertEquals(utbetalingsperiode, sorterteVedtaksperioder[3])

        // Avslag uten datoer legger seg til slutt
        Assertions.assertEquals(avslagUtenDatoer, sorterteVedtaksperioder[4])
    }
}
