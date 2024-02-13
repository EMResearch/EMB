package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.RestSanityEØSBegrunnelse
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.net.URI

const val sanityBaseUrl = "https://xsrv1mh6.api.sanity.io/v2021-06-07/data/query"

@Component
class SanityKlient(
    @Value("\${SANITY_DATASET}") private val datasett: String,
    private val restTemplate: RestTemplate,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = OpprettTaskService.RETRY_BACKOFF_5000MS),
    )
    fun hentBegrunnelser(): List<SanityBegrunnelse> {
        val sanityUrl = "$sanityBaseUrl/$datasett"
        val hentBegrunnelserQuery = java.net.URLEncoder.encode(hentBegrunnelser, "utf-8")

        val uri = URI.create("$sanityUrl?query=$hentBegrunnelserQuery")

        val restSanityBegrunnelser =
            kallEksternTjeneste(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter begrunnelser fra sanity",
            ) {
                restTemplate.getForEntity<SanityBegrunnelserRespons>(uri).body?.result
                    ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")
            }

        return restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = OpprettTaskService.RETRY_BACKOFF_5000MS),
    )
    fun hentEØSBegrunnelser(): List<SanityEØSBegrunnelse> {
        val sanityUrl = "$sanityBaseUrl/$datasett"
        val hentEØSBegrunnelserQuery = java.net.URLEncoder.encode(hentEØSBegrunnelser, "utf-8")

        val uri = URI.create("$sanityUrl?query=$hentEØSBegrunnelserQuery")

        return kallEksternTjeneste(
            tjeneste = "Sanity",
            uri = uri,
            formål = "Henter EØS-begrunnelser fra sanity",
        ) {
            restTemplate.getForEntity<SanityEØSBegrunnelserRespons>(uri).body?.result
                ?.mapNotNull { it.tilSanityEØSBegrunnelse() }
                ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")
        }
    }
}

data class SanityBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<RestSanityBegrunnelse>,
)

data class SanityEØSBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<RestSanityEØSBegrunnelse>,
)
