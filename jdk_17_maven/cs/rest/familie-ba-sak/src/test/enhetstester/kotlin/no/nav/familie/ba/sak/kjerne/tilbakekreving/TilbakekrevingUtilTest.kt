package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class TilbakekrevingUtilTest {

    private val fom1 = LocalDate.of(2020, 1, 1)
    private val tom1 = LocalDate.of(2020, 1, 31)
    private val tom2 = LocalDate.of(2020, 2, 28)
    private val fom3 = LocalDate.of(2020, 4, 1)
    private val tom3 = LocalDate.of(2020, 4, 30)
    private val fom4 = LocalDate.of(2020, 5, 1)
    private val tom4 = LocalDate.of(2020, 5, 31)

    @Test
    fun `test validerVerdierPåRestTilbakekreving kaster feil ved tilbakekreving uten feilutbetaling`() {
        assertThrows<Exception> {
            validerVerdierPåRestTilbakekreving(
                restTilbakekreving = RestTilbakekreving(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "",
                ),
                feilutbetaling = BigDecimal.ZERO,
            )
        }
    }

    @Test
    fun `test validerVerdierPåRestTilbakekreving kaster feil ved ingen tilbakekreving når det er en feilutbetaling`() {
        assertThrows<Exception> {
            validerVerdierPåRestTilbakekreving(
                restTilbakekreving = null,
                feilutbetaling = BigDecimal.ONE,
            )
        }
    }

    @Test
    fun `test sammenslåing av feilutbetalingsperioder med ensom siste periode`() {
        val simuleringsPerioder = listOf(
            opprettSimuleringsPeriode(
                fom = fom1,
                tom = tom1,
                feilUtbetaling = BigDecimal.ONE,
            ),
            opprettSimuleringsPeriode(
                fom = LocalDate.of(2020, 2, 1),
                tom = tom2,
                feilUtbetaling = BigDecimal.ONE,
            ),
            opprettSimuleringsPeriode(
                fom = LocalDate.of(2020, 3, 1),
                tom = LocalDate.of(2020, 3, 31),
                feilUtbetaling = BigDecimal.ZERO,
            ),
            opprettSimuleringsPeriode(
                fom = fom4,
                tom = tom4,
                feilUtbetaling = BigDecimal.ONE,
            ),
        )

        val sammenslåttePerioder = slåsammenNærliggendeFeilutbtalingPerioder(simuleringsPerioder)

        Assertions.assertEquals(2, sammenslåttePerioder.size)
        Assertions.assertEquals(fom1, sammenslåttePerioder[0].fom)
        Assertions.assertEquals(tom2, sammenslåttePerioder[0].tom)
        Assertions.assertEquals(fom4, sammenslåttePerioder[1].fom)
        Assertions.assertEquals(tom4, sammenslåttePerioder[1].tom)
    }

    @Test
    fun `test sammenslåing av feilutbetalingsperioder med ensom første periode`() {
        val simuleringsPerioder = listOf(
            opprettSimuleringsPeriode(
                fom = fom1,
                tom = tom1,
                feilUtbetaling = BigDecimal.ONE,
            ),
            opprettSimuleringsPeriode(
                fom = LocalDate.of(2020, 2, 1),
                tom = tom2,
                feilUtbetaling = BigDecimal.ZERO,
            ),
            opprettSimuleringsPeriode(
                fom = fom3,
                tom = tom3,
                feilUtbetaling = BigDecimal.ONE,
            ),
            opprettSimuleringsPeriode(
                fom = fom4,
                tom = tom4,
                feilUtbetaling = BigDecimal.ONE,
            ),
        )

        val sammenslåttePerioder = slåsammenNærliggendeFeilutbtalingPerioder(simuleringsPerioder)

        Assertions.assertEquals(2, sammenslåttePerioder.size)
        Assertions.assertEquals(fom1, sammenslåttePerioder[0].fom)
        Assertions.assertEquals(tom1, sammenslåttePerioder[0].tom)
        Assertions.assertEquals(fom3, sammenslåttePerioder[1].fom)
        Assertions.assertEquals(tom4, sammenslåttePerioder[1].tom)
    }

    private fun opprettSimuleringsPeriode(
        fom: LocalDate,
        tom: LocalDate,
        feilUtbetaling: BigDecimal,
    ): SimuleringsPeriode = SimuleringsPeriode(
        fom = fom,
        tom = tom,
        feilutbetaling = feilUtbetaling,
        forfallsdato = LocalDate.now(),
        manuellPostering = BigDecimal.ZERO,
        nyttBeløp = BigDecimal.ZERO,
        tidligereUtbetalt = BigDecimal.ZERO,
        resultat = BigDecimal.ZERO,
        etterbetaling = BigDecimal.ZERO,
    )
}
