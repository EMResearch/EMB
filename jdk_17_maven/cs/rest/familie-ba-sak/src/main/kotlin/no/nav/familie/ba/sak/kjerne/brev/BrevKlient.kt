package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

val FAMILIE_BREV_TJENESTENAVN = "famile-brev"

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "familie-brev") {

    fun genererBrev(målform: String, brev: Brev): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        return kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Hente pdf for vedtaksbrev") {
            postForEntity(uri, brev.data)
        }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseMedData): String {
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")

        return kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Henter begrunnelsestekst") {
            postForEntity(uri, begrunnelseData)
        }
    }
}
