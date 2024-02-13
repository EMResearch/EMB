package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenSammenhengendePerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InternPeriodeOvergangsstønadTest {
    @Test
    fun `Skal slå sammen perioder som er sammenhengende`() {
        val personIdent = randomFnr()
        val sammenslåttePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(3).sisteDagIMåned(),
                personIdent = personIdent,
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().sisteDagIMåned(),
                personIdent = personIdent,
            ),
        ).slåSammenSammenhengendePerioder()

        assertEquals(1, sammenslåttePerioder.size)
    }

    @Test
    fun `Skal ikke slå sammen perioder som ikke er sammenhengende`() {
        val personIdent = randomFnr()
        val sammenslåttePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(4).sisteDagIMåned(),
                personIdent = personIdent,
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().sisteDagIMåned(),
                personIdent = personIdent,
            ),
        ).slåSammenSammenhengendePerioder()

        assertEquals(2, sammenslåttePerioder.size)
    }
}
