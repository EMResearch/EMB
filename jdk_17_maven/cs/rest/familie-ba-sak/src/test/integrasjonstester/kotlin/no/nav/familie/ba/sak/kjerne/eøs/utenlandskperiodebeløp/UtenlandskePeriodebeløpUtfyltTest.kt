package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagUtenlandskPeriodebeløp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UtenlandskePeriodebeløpUtfyltTest {

    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp(
            beløp = BigDecimal.valueOf(500),
            valutakode = "NOK",
            intervall = Intervall.MÅNEDLIG,
        )

        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp()

        assertEquals(UtfyltStatus.OK, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        var utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp(
            beløp = BigDecimal.valueOf(500),
        )

        var restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)

        utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp(
            beløp = BigDecimal.valueOf(500),
            valutakode = "NOK",
        )

        restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp()

        assertEquals(UtfyltStatus.IKKE_UTFYLT, restUtenlandskPeriodebeløp.status)
    }
}
