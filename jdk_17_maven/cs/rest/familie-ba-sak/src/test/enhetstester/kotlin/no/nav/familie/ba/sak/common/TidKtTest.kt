package no.nav.familie.ba.sak.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

internal class TidKtTest {

    @Test
    fun `Test YearMonth range over flere måneder`() {
        assertEquals(4, (YearMonth.of(2021, 1)..YearMonth.of(2021, 4)).toList().size)
    }

    @Test
    fun `Test YearMonth range med én måned`() {
        assertEquals(1, (YearMonth.of(2021, 1)..YearMonth.of(2021, 1)).toList().size)
    }

    @Test
    fun `Test YearMonth range med tidligere sluttmåned`() {
        assertEquals(0, (YearMonth.of(2021, 1)..YearMonth.of(2020, 11)).toList().size)
    }

    @Test
    fun `Test YearMonth range med tidligere sluttmåned og negativt steg`() {
        assertEquals(3, (YearMonth.of(2021, 1)..YearMonth.of(2020, 11) step -1).toList().size)
    }

    @Test
    fun `Test YearMonth range med senere sluttmåned og negativt steg`() {
        assertEquals(0, (YearMonth.of(2021, 1)..YearMonth.of(2021, 11) step -1).toList().size)
    }

    @Test
    fun `Test YearMonth range med tidligere sluttmåned og null som steg`() {
        assertThrows<IllegalStateException> { (YearMonth.of(2021, 1)..YearMonth.of(2020, 11) step 0).toList() }
    }
}
