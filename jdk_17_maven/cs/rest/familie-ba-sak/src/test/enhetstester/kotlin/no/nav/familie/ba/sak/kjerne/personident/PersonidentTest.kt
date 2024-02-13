package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class PersonidentTest {

    val fnr1 = "12345678903"
    val fnr2 = "23345678903"

    @Test
    fun `To personidenter er like hvis de har samme fødselsnummer og aktiv`() {
        val p1 = Personident(fnr1, aktiv = true, aktør = mockk())
        val p2 = Personident(fnr1, aktiv = true, aktør = mockk())
        assertEquals(p1, p2)
    }

    @Test
    fun `To personidenter er ulike hvis de har samme fødselsnummer, men kun en er aktiv`() {
        val p1 = Personident(fnr1, aktiv = true, aktør = mockk())
        val p2 = Personident(fnr1, aktiv = false, aktør = mockk())
        assertNotEquals(p1, p2)
    }

    @Test
    fun `To personidenter er like hvis de har samme fødselsnummer og begge er inaktive`() {
        val p1 = Personident(fnr1, aktiv = false, aktør = mockk())
        val p2 = Personident(fnr1, aktiv = false, aktør = mockk())
        assertEquals(p1, p2)
    }

    @Test
    fun `To personidenter er ulike hvis de har forskjellige fødselsnummer og begge er aktive`() {
        val p1 = Personident(fnr1, aktiv = true, aktør = mockk())
        val p2 = Personident(fnr2, aktiv = true, aktør = mockk())
        assertNotEquals(p1, p2)
    }
}
