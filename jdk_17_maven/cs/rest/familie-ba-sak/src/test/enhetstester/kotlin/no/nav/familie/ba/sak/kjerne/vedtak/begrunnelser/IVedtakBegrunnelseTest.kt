package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IVedtakBegrunnelseTest {
    @Test
    fun `Skal serialiseres med prefix`() {
        val serialisertStandardbegrunnelse =
            objectMapper.writeValueAsString(Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER)
        Assertions.assertEquals(
            objectMapper.readValue(serialisertStandardbegrunnelse, Standardbegrunnelse::class.java),
            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER,
        )

        val serialisertEØSStandardbegrunnelse =
            objectMapper.writeValueAsString(EØSStandardbegrunnelse.AVSLAG_EØS_IKKE_EØS_BORGER)
        Assertions.assertEquals(
            objectMapper.readValue(serialisertEØSStandardbegrunnelse, EØSStandardbegrunnelse::class.java),
            EØSStandardbegrunnelse.AVSLAG_EØS_IKKE_EØS_BORGER,
        )
    }
}
