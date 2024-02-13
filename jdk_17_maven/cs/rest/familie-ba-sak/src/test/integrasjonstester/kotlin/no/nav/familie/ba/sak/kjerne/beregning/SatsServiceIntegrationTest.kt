package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.ORBA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.SMA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.TILLEGG_ORBA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.eøs.util.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.ogSenere
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.ogTidligere
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.plus
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SatsServiceIntegrationTest : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal gi riktig sats for ordinær barnetrygd, 6 til 18 år`() {
        val forventet =
            feb(2019).ogTidligere().tilTidslinje { 970 } +
                (mar(2019)..feb(2023)).tilTidslinje { 1054 } +
                (mar(2023)..jun(2023)).tilTidslinje { 1083 } +
                jul(2023).ogSenere().tilTidslinje { 1310 }

        val faktisk = satstypeTidslinje(ORBA)

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `Skal gi riktig sats for tillegg ordinær barnetrygd, 0 til 6 år`() {
        val forventet =
            feb(2019).ogTidligere().tilTidslinje { 970 } +
                (mar(2019)..aug(2020)).tilTidslinje { 1054 } +
                (sep(2020)..aug(2021)).tilTidslinje { 1354 } +
                (sep(2021)..des(2021)).tilTidslinje { 1654 } +
                (jan(2022)..feb(2023)).tilTidslinje { 1676 } +
                (mar(2023)..jun(2023)).tilTidslinje { 1723 } +
                jul(2023).ogSenere().tilTidslinje { 1766 }

        val faktisk = satstypeTidslinje(TILLEGG_ORBA)

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `Skal gi riktig sats for småbarnstillegg`() {
        val forventet =
            feb(2023).ogTidligere().tilTidslinje { 660 } +
                (mar(2023)..jun(2023)).tilTidslinje { 678 } +
                jul(2023).ogSenere().tilTidslinje { 696 }

        val faktisk = satstypeTidslinje(SMA)

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `Skal gi riktig sats for utvidet barnetrygd`() {
        val forventet =
            feb(2019).ogTidligere().tilTidslinje { 970 } +
                (mar(2019)..feb(2023)).tilTidslinje { 1054 } +
                (mar(2023)..jun(2023)).tilTidslinje { 2489 } +
                jul(2023).ogSenere().tilTidslinje { 2516 }

        val faktisk = satstypeTidslinje(UTVIDET_BARNETRYGD)

        assertEquals(forventet, faktisk)
    }
}
