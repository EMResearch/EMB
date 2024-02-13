package no.nav.familie.ba.sak.integrasjoner.ef

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class EfSakRestClient(
    @Value("\${FAMILIE_EF_SAK_API_URL}") private val efSakBaseUrl: URI,
    @Qualifier("jwtBearer") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "ef-sak") {

    fun hentPerioderMedFullOvergangsstønad(personIdent: String): EksternePerioderResponse {
        val uri = UriUtil.uri(efSakBaseUrl, "ekstern/perioder/full-overgangsstonad")

        return kallEksternTjeneste<Ressurs<EksternePerioderResponse>>(
            tjeneste = "ef-sak overgangsstønad",
            uri = uri,
            formål = "Hente perioder med full overgangsstønad",
        ) { postForEntity(uri, PersonIdent(personIdent)) }.getDataOrThrow()
    }
}
