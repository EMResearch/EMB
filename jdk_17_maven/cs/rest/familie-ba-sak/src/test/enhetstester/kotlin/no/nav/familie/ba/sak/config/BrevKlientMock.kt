package no.nav.familie.ba.sak.config

import io.mockk.spyk
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class BrevKlientMock : BrevKlient(
    familieBrevUri = "brev_uri_mock",
    restTemplate = RestTemplate(),
    sanityDataset = "",
) {

    override fun genererBrev(målform: String, brev: Brev): ByteArray {
        return TEST_PDF
    }

    override fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseMedData): String {
        return "Dummytekst for ${begrunnelseData.apiNavn}"
    }
}

@TestConfiguration
class BrevKlientTestFactory {

    @Bean
    @Profile("mock-brev-klient")
    @Primary
    fun brevKlient() = spyk<BrevKlientMock>()
}
