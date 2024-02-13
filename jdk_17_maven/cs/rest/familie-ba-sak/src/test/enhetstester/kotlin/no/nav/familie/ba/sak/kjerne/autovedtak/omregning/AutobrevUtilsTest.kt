package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutobrevUtilsTest {

    @Test
    fun `Skal sjekke at historiske og gjeldene begrunnelser blir hentet for 6 år`() {
        val begrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(alder = 6)

        assertEquals(listOf("REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK", "REDUKSJON_UNDER_6_ÅR"), begrunnelser.map { it.name })
    }

    @Test
    fun `Skal sjekke at historiske og gjeldene begrunnelser blir hentet for 18 år`() {
        val begrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(alder = 18)

        assertEquals(listOf("REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK", "REDUKSJON_UNDER_18_ÅR"), begrunnelser.map { it.name })
    }

    @Test
    fun `Skal sjekke at gjeldende begrunnelse for autobrev er i listen over alle`() {
        assertTrue(
            AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(6)
                .contains(AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(6)),
        )

        assertTrue(
            AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(18)
                .contains(AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(18)),
        )
    }
}
