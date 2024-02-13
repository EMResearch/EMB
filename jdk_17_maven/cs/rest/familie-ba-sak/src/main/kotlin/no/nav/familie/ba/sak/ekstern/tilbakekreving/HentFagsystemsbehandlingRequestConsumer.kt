package no.nav.familie.ba.sak.ekstern.tilbakekreving

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class HentFagsystemsbehandlingRequestConsumer(private val fagsystemsbehandlingService: FagsystemsbehandlingService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    var latch: CountDownLatch = CountDownLatch(1)

    @KafkaListener(
        id = "familie-ba-sak",
        topics = ["teamfamilie.privat-tbk-hentfagsystemsbehandling-request-topic"],
        containerFactory = "concurrentKafkaListenerContainerFactory",
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val data: String = consumerRecord.value()
        val key: String = consumerRecord.key()
        val request: HentFagsystemsbehandlingRequest =
            objectMapper.readValue(data, HentFagsystemsbehandlingRequest::class.java)

        if (request.ytelsestype != Ytelsestype.BARNETRYGD) {
            return
        }
        logger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord")
        secureLogger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord")

        val fagsystemsbehandling = try {
            fagsystemsbehandlingService.hentFagsystemsbehandling(request)
        } catch (e: Exception) {
            logger.warn(
                "Noe gikk galt mens sender HentFagsystemsbehandlingRespons for behandling=${request.eksternId}. " +
                    "Feiler med ${e.message}",
            )
            HentFagsystemsbehandlingRespons(feilMelding = e.message)
        }
        fagsystemsbehandlingService.sendFagsystemsbehandling(fagsystemsbehandling, key, request.eksternId)
        latch.countDown()
        ack.acknowledge()
    }
}
