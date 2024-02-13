package no.nav.familie.ba.sak.integrasjoner.samhandler

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfo
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfoRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class SamhandlerKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "samhandler") {

    @Cacheable("hent-samhandler", cacheManager = "dailyCache")
    fun hentSamhandler(orgNummer: String): SamhandlerInfo {
        val uri = URI.create("$familieOppdragUri/tss/orgnr/$orgNummer")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter samhandler fra TSS",
        ) {
            getForEntity(uri = uri)
        }
    }

    fun søkSamhandlere(navn: String?, postnummer: String?, område: String?, side: Int): SøkSamhandlerInfo {
        val uri = URI.create("$familieOppdragUri/tss/navn")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Søk samhandler fra TSS",
        ) {
            postForEntity(uri = uri, SøkSamhandlerInfoRequest(navn, side, postnummer, område))
        }
    }
}
