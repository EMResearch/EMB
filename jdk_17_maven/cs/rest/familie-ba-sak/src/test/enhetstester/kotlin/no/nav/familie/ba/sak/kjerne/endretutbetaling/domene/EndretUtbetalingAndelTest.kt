package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTomIFremtiden
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class EndretUtbetalingAndelTest {

    @Test
    fun `Sjekk validering med tomme felt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)
        endretUtbetalingAndel.begrunnelse = ""

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.person = tilfeldigPerson()
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med ikke tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.person = tilfeldigPerson()
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.avtaletidspunktDeltBosted = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            fom = YearMonth.now(),
            tom = null,
            årsak = Årsak.DELT_BOSTED,
        )

        val sisteTomPåAndeler = YearMonth.now().plusMonths(10)
        val andelTilkjentYtelser = listOf(
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(5),
            ),
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().minusMonths(4),
                tom = YearMonth.now().plusMonths(4),
            ),
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().plusMonths(5),
                tom = sisteTomPåAndeler,
            ),
        )

        val nyTom = beregnGyldigTomIFremtiden(
            andelTilkjentYtelser = andelTilkjentYtelser,
            endretUtbetalingAndel = endretUtbetalingAndel,
            andreEndredeAndelerPåBehandling = emptyList(),
        )

        assertEquals(sisteTomPåAndeler, nyTom)
    }

    @Test
    fun `Skal sette tom til måneden før neste endringsperiode`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            fom = YearMonth.now(),
            tom = null,
            årsak = Årsak.DELT_BOSTED,
        )

        val annenEndretAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            fom = YearMonth.now().plusMonths(5),
            tom = YearMonth.now().plusMonths(8),
            årsak = Årsak.DELT_BOSTED,
        )

        val andelTilkjentYtelser = listOf(
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(5),
            ),
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().minusMonths(4),
                tom = YearMonth.now().plusMonths(4),
            ),
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().plusMonths(5),
                tom = YearMonth.now().plusMonths(10),
            ),
        )

        val nyTom = beregnGyldigTomIFremtiden(
            andelTilkjentYtelser = andelTilkjentYtelser,
            endretUtbetalingAndel = endretUtbetalingAndel,
            andreEndredeAndelerPåBehandling = listOf(annenEndretAndel),
        )

        assertEquals(annenEndretAndel.fom!!.minusMonths(1), nyTom)
    }
}
