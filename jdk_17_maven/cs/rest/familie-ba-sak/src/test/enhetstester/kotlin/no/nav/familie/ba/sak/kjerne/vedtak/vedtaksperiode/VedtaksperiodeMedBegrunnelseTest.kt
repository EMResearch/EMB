package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.erAlleredeBegrunnetMedBegrunnelse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeMedBegrunnelseTest {

    @Test
    fun `Skal finne begrunnelse på tidligere vedtaksperiode for samme fra- og med dato`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = fom.førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                        Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(
                Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR,
                Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK,
            ),
            måned = fom,
        )

        assertTrue(fagsakErBegrunnet)
    }

    @Test
    fun `Skal ikke finne begrunnelse på tidligere vedtaksperiode når den er begrunnet for 1 år siden`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = fom.minusYears(1).førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                        Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK),
            måned = fom,
        )

        assertFalse(fagsakErBegrunnet)
    }

    @Test
    fun `Skal ikke finne begrunnelse på tidligere vedtaksperiode når den ikke har begrunnelsen i det hele tatt`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = fom.minusYears(1).førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK),
            måned = fom,
        )

        assertFalse(fagsakErBegrunnet)
    }

    @Test
    fun `Skal være begrunnet med reduksjon 3 år småbarnstillegg fra før`() {
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR),
            måned = YearMonth.now(),
        )

        assertTrue(fagsakErBegrunnet)
    }

    @Test
    fun `Skal være begrunnet med reduksjon 3 år småbarnstillegg for 1 år siden, men sende ut brev for neste barn som fyller 3 år`() {
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().minusYears(1).førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR),
            måned = YearMonth.now(),
        )

        assertFalse(fagsakErBegrunnet)
    }

    @Test
    fun `Skal være begrunnet med reduksjon ikke lenger full OS`() {
        val vedtak = lagVedtak()

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        val fagsakErBegrunnet = vedtaksperioder.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = listOf(Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD),
            måned = YearMonth.now(),
        )

        assertTrue(fagsakErBegrunnet)
    }
}
