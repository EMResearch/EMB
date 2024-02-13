package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

internal class EndretUtbetalingAndelTidslinjeServiceTest {

    @Test
    fun `lager tidslinje for ett barn med én etterbetaling`() {
        val person = tilfeldigPerson()
        val endringer = listOf(
            lagEndretUtbetalingAndel(
                person = person,
                årsak = Årsak.ETTERBETALING_3ÅR,
                fom = YearMonth.of(2020, 3),
                tom = YearMonth.of(2020, 7),
                prosent = BigDecimal.ZERO,
            ),
        )

        val forventet = mapOf(
            person.aktør to "TTTTT".somBoolskTidslinje(mar(2020)),
        )

        val faktisk = endringer.tilBarnasSkalIkkeUtbetalesTidslinjer()

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `lager tidslinje for to barn med flere etterbetalinger`() {
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val endringer = listOf(
            lagEndretUtbetalingAndel(
                person = person1,
                årsak = Årsak.ETTERBETALING_3ÅR,
                fom = YearMonth.of(2020, 3),
                tom = YearMonth.of(2020, 7),
                prosent = BigDecimal.ZERO,
            ),
            lagEndretUtbetalingAndel(
                person = person2,
                årsak = Årsak.ETTERBETALING_3ÅR,
                fom = YearMonth.of(2019, 11),
                tom = YearMonth.of(2021, 3),
                prosent = BigDecimal.ZERO,
            ),
            lagEndretUtbetalingAndel(
                person = person1,
                årsak = Årsak.ETTERBETALING_3ÅR,
                fom = YearMonth.of(2021, 1),
                tom = YearMonth.of(2021, 5),
                prosent = BigDecimal.ZERO,
            ),
        )

        val forventet = mapOf(
            person1.aktør to "TTTTT     TTTTT".somBoolskTidslinje(mar(2020)).filtrerIkkeNull(),
            person2.aktør to "TTTTTTTTTTTTTTTTT".somBoolskTidslinje(nov(2019)).filtrerIkkeNull(),
        )

        val faktisk = endringer.tilBarnasSkalIkkeUtbetalesTidslinjer()

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `lager tidslinje for ett barn med allerede utbetalt`() {
        val person = tilfeldigPerson()
        val endringer = listOf(
            lagEndretUtbetalingAndel(
                person = person,
                årsak = Årsak.ALLEREDE_UTBETALT,
                fom = YearMonth.of(2020, 3),
                tom = YearMonth.of(2020, 7),
                prosent = BigDecimal.ZERO,
            ),
        )

        val forventet = mapOf(
            person.aktør to "TTTTT".somBoolskTidslinje(mar(2020)),
        )

        val faktisk = endringer.tilBarnasSkalIkkeUtbetalesTidslinjer()

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `lager tidslinje for ett barn med endre mottaker`() {
        val person = tilfeldigPerson()
        val endringer = listOf(
            lagEndretUtbetalingAndel(
                person = person,
                årsak = Årsak.ENDRE_MOTTAKER,
                fom = YearMonth.of(2020, 3),
                tom = YearMonth.of(2020, 7),
                prosent = BigDecimal.ZERO,
            ),
        )

        val forventet = mapOf(
            person.aktør to "TTTTT".somBoolskTidslinje(mar(2020)),
        )

        val faktisk = endringer.tilBarnasSkalIkkeUtbetalesTidslinjer()

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `ikke lag tidslinje hvis årsaken ikke er etterbetaling 3 år, allerede utbetalt eller endre mottaker`() {
        val person = tilfeldigPerson()
        val endringer = listOf(
            lagEndretUtbetalingAndel(
                person = person,
                årsak = Årsak.DELT_BOSTED,
                fom = YearMonth.of(2020, 3),
                tom = YearMonth.of(2020, 7),
            ),
        )

        val faktisk = endringer.tilBarnasSkalIkkeUtbetalesTidslinjer()

        assertEquals(emptyMap<Aktør, Tidslinje<Boolean, Måned>>(), faktisk)
    }
}
