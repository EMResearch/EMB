package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class SanityKlientMock {
    @Bean
    @Profile("mock-sanity-client")
    @Primary
    fun mockSanityClient(): SanityKlient {
        return testSanityKlient
    }
}

val testSanityKlient = TestSantityKlient()

class TestSantityKlient : SanityKlient("ba-brev", restTemplate) {
    private val begrunnelser: List<SanityBegrunnelse> by lazy {
        super.hentBegrunnelser()
    }
    private val eøsBegrunnelser: List<SanityEØSBegrunnelse> by lazy {
        super.hentEØSBegrunnelser()
    }

    override fun hentBegrunnelser(): List<SanityBegrunnelse> {
        return begrunnelser
    }

    override fun hentEØSBegrunnelser(): List<SanityEØSBegrunnelse> {
        return eøsBegrunnelser
    }

    fun hentBegrunnelserMap(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val enumVerdier = Standardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val begrunnelser = hentBegrunnelser()
        return tilMap(begrunnelser, enumVerdier)
    }

    fun hentEØSBegrunnelserMap(): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val enumVerdier = EØSStandardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val begrunnelser = hentEØSBegrunnelser()
        return tilMap(begrunnelser, enumVerdier)
    }

    private fun <SANITY_BEGRUNNELSE : ISanityBegrunnelse, VEDTAK_BEGRUNNELSE : IVedtakBegrunnelse> tilMap(
        begrunnelser: List<SANITY_BEGRUNNELSE>,
        enumVerdier: Map<String, VEDTAK_BEGRUNNELSE>,
    ): Map<VEDTAK_BEGRUNNELSE, SANITY_BEGRUNNELSE> =
        begrunnelser.mapNotNull { sanityBegrunnelse ->
            enumVerdier[sanityBegrunnelse.apiNavn]?.let { it to sanityBegrunnelse }
        }.toMap()
}
