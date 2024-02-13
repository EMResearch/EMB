package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MånedFraMånedsskifteTest {

    @Test
    fun `skal gi tom tidslinje hvis alle dager er inni én måned`() {
        val daglinje = "aaaaaa".tilCharTidslinje(7.des(2021))
        val månedtidsline = daglinje
            .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, verdiFørsteDagDenneMåned -> 'b' }

        assertEquals(TomTidslinje<Char, Måned>(), månedtidsline)
    }

    @Test
    fun `skal gi én måned ved ett månedsskifte`() {
        val daglinje = "abcdefg".tilCharTidslinje(28.nov(2021))
        val månedtidsline = daglinje
            .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, verdiFørsteDagDenneMåned ->
                verdiFørsteDagDenneMåned
            }

        assertEquals("d".tilCharTidslinje(des(2021)), månedtidsline)
    }

    @Test
    fun `skal gi to måneder ved to månedsskifter`() {
        val daglinje = "abcdefghijklmnopqrstuvwxyzæøå0123456789".tilCharTidslinje(28.nov(2021))
        val månedtidsline = daglinje
            .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, verdiFørsteDagDenneMåned ->
                verdiSisteDagForrigeMåned
            }

        assertEquals("c4".tilCharTidslinje(des(2021)), månedtidsline)
    }

    @Test
    fun `skal gi tom tidslinje hvis månedsskiftet mangler verdi på begge sider`() {
        val daglinje = "abcdefghijklmnopqrstuvwxyzæøå0123456789".tilCharTidslinje(28.nov(2021))
            .mapIkkeNull {
                when (it) {
                    'c', 'd', '4', '5' -> null // 30/11, 1/12, 31/12 og 1/1 mangler verdi
                    else -> it
                }
            }

        val månedtidsline = daglinje
            .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, verdiFørsteDagDenneMåned -> 'A' }

        assertEquals(TomTidslinje<Char, Måned>(), månedtidsline)
    }

    @Test
    fun `skal gi tom tidslinje hvis månedsskiftet mangler verdi på begge én av sidene`() {
        val daglinje = "abcdefghijklmnopqrstuvwxyzæøå0123456789".tilCharTidslinje(28.nov(2021))
            .mapIkkeNull {
                when (it) {
                    'c', '5' -> null // 30/11 og 1/1 mangler verdi
                    else -> it
                }
            }

        val månedtidsline = daglinje
            .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, verdiFørsteDagDenneMåned -> 'A' }

        assertEquals(TomTidslinje<Char, Måned>(), månedtidsline)
    }
}
