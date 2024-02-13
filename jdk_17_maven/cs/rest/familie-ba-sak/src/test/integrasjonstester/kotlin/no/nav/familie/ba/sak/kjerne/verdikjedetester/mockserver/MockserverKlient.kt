package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestOperations
import org.springframework.web.client.postForEntity

class MockserverKlient(
    private val mockServerUrl: String,
    private val restOperations: RestOperations,
) {
    fun lagScenario(restScenario: RestScenario): RestScenario {
        val scenario = restOperations.postForEntity<RestScenario>("$mockServerUrl/rest/scenario", restScenario).body
            ?: error("Klarte ikke lage scenario med data $restScenario")
        logger.info("Laget scenario: ${scenario.convertDataClassToJson()}")

        return scenario
    }

    companion object {

        val logger = LoggerFactory.getLogger(MockserverKlient::class.java)
    }
}
