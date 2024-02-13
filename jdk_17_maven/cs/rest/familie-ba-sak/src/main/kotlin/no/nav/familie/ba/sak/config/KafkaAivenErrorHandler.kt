package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.secureLogger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class KafkaAivenErrorHandler : CommonContainerStoppingErrorHandler() {

    val logger: Logger = LoggerFactory.getLogger(KafkaAivenErrorHandler::class.java)

    private val executor: Executor
    private val teller = AtomicInteger(0)
    private val sisteFeil = AtomicLong(0)
    override fun handleRemaining(
        e: Exception,
        records: List<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        if (records.isNullOrEmpty()) {
            logger.error("Feil ved konsumering av melding. Ingen records. ${consumer.subscription()}", e)
            scheduleRestart(
                e,
                records,
                consumer,
                container,
                "Ukjent topic",
            )
        } else {
            records.first().run {
                logger.error(
                    "Feil ved konsumering av melding fra ${this.topic()}. id ${this.key()}, " +
                        "offset: ${this.offset()}, partition: ${this.partition()}",
                )
                secureLogger.error("${this.topic()} - Problemer med prosessering av $records", e)
                scheduleRestart(
                    e,
                    records,
                    consumer,
                    container,
                    this.topic(),
                )
            }
        }
    }

    private fun scheduleRestart(
        e: Exception,
        records: List<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        topic: String,
    ) {
        val now = System.currentTimeMillis()
        if (now - sisteFeil.getAndSet(now) > COUNTER_RESET_TID) {
            teller.set(0)
        }
        val numErrors = teller.incrementAndGet()
        val stopTime =
            if (numErrors > MAKS_ANTALL_FEIL) MAKS_STOP_TID else MIN_STOP_TID * numErrors
        executor.execute {
            try {
                Thread.sleep(stopTime)
                logger.warn("Starter kafka container for $topic")
                container.start()
            } catch (exception: Exception) {
                logger.error("Feil oppstod ved venting og oppstart av kafka container", exception)
            }
        }
        logger.warn("Stopper kafka container for $topic i ${Duration.ofMillis(stopTime)}")
        super.handleRemaining(
            Exception("Sjekk securelogs for mer info - ${e::class.java.simpleName}"),
            records,
            consumer,
            container,
        )
    }

    companion object {

        private val MAKS_STOP_TID = Duration.ofHours(3).toMillis()
        private val MIN_STOP_TID = Duration.ofSeconds(20).toMillis()
        private const val MAKS_ANTALL_FEIL = 10
        private val COUNTER_RESET_TID = MIN_STOP_TID * MAKS_ANTALL_FEIL * 2
    }

    init {
        this.executor = SimpleAsyncTaskExecutor()
    }
}
