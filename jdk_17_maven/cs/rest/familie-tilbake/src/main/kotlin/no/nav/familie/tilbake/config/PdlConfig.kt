package no.nav.familie.tilbake.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(@Value("\${PDL_URL}") pdlUrl: URI) {

    val pdlUri: URI = UriComponentsBuilder.fromUri(pdlUrl).path(PATH_GRAPHQL).build().toUri()

    companion object {

        const val PATH_GRAPHQL = "graphql"

        val hentEnkelPersonQuery = graphqlQuery("hentperson-enkel")
        val hentIdenterQuery = graphqlQuery("hentIdenter")
        val hentAdressebeskyttelseBolkQuery = graphqlQuery("hent-adressebeskyttelse-bolk")

        private fun graphqlQuery(pdlResource: String) = PdlConfig::class.java.getResource("/pdl/$pdlResource.graphql")
            .readText()
            .graphqlCompatible()

        private fun String.graphqlCompatible(): String {
            return StringUtils.normalizeSpace(this.replace("\n", ""))
        }
    }
}
