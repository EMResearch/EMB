package no.nav.familie.tilbake.behandling.consumer

import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.config.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CountDownLatch

@Service
@Profile("!integrasjonstest & !e2e")
class HentFagsystemsbehandlingResponsConsumer(private val fagsystemsbehandlingService: HentFagsystemsbehandlingService) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    var latch: CountDownLatch = CountDownLatch(1)

    @KafkaListener(
        id = "familie-tilbake",
        topics = [KafkaConfig.HENT_FAGSYSTEMSBEHANDLING_RESPONS_TOPIC],
        containerFactory = "concurrentKafkaListenerContainerFactory",
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        logger.info("Fagsystemsbehandlingsdata er mottatt i kafka med key=${consumerRecord.key()}")
        secureLogger.info("Fagsystemsbehandlingsdata er mottatt i kafka $consumerRecord")

        val requestId = UUID.fromString(consumerRecord.key())
        val data: String = consumerRecord.value()
        fagsystemsbehandlingService.lagreHentFagsystemsbehandlingRespons(requestId, data)
        latch.countDown()
        ack.acknowledge()
    }
}
