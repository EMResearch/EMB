package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.config.testSanityKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SanityKlientTest {

    @Test
    fun `Skal teste at vi klarer å hente begrunnelser fra sanity-apiet`() {
        val hentBegrunnelser = testSanityKlient.hentBegrunnelser()
        val begrunnelserPåApiNavn = hentBegrunnelser.associateBy { it.apiNavn }
        assertThat(hentBegrunnelser).hasSize(begrunnelserPåApiNavn.size)
        assertThat(hentBegrunnelser).isNotEmpty
    }

    @Test
    fun `Skal teste at vi klarer å hente eøs-begrunnelser fra sanity-apiet`() {
        val hentEØSBegrunnelser = testSanityKlient.hentEØSBegrunnelser()
        val begrunnelserPåApiNavn = hentEØSBegrunnelser.associateBy { it.apiNavn }
        assertThat(hentEØSBegrunnelser).hasSize(begrunnelserPåApiNavn.size)
        assertThat(hentEØSBegrunnelser).isNotEmpty
    }
}
