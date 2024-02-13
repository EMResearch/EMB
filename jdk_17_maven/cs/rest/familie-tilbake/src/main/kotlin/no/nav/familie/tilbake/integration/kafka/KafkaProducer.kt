package no.nav.familie.tilbake.integration.kafka

import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.KafkaConfig
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

interface KafkaProducer {

    fun sendHistorikkinnslag(behandlingId: UUID, key: String, request: OpprettHistorikkinnslagRequest)
    fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand)
    fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering)
    fun sendHentFagsystemsbehandlingRequest(requestId: UUID, request: HentFagsystemsbehandlingRequest)
}

@Service
@Profile("!integrasjonstest & !e2e")
class DefaultKafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) : KafkaProducer {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun sendHistorikkinnslag(behandlingId: UUID, key: String, request: OpprettHistorikkinnslagRequest) {
        sendKafkamelding(behandlingId, KafkaConfig.HISTORIKK_TOPIC, key, request)
    }

    override fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand) {
        sendKafkamelding(behandlingId, KafkaConfig.SAK_TOPIC, request.behandlingUuid.toString(), request)
    }

    override fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering) {
        sendKafkamelding(behandlingId, KafkaConfig.VEDTAK_TOPIC, request.behandlingUuid.toString(), request)
    }

    override fun sendHentFagsystemsbehandlingRequest(requestId: UUID, request: HentFagsystemsbehandlingRequest) {
        sendKafkamelding(requestId, KafkaConfig.HENT_FAGSYSTEMSBEHANDLING_REQUEST_TOPIC, requestId.toString(), request)
    }

    private fun sendKafkamelding(behandlingId: UUID, topic: String, key: String, request: Any) {
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(topic, key, melding)
        kotlin.runCatching {
            val callback = kafkaTemplate.send(producerRecord).get()
            log.info(
                "Melding på topic $topic for $behandlingId med $key er sendt. " +
                    "Fikk offset ${callback?.recordMetadata?.offset()}",
            )
        }.onFailure {
            val feilmelding = "Melding på topic $topic kan ikke sendes for $behandlingId med $key. " +
                "Feiler med ${it.message}"
            log.warn(feilmelding)
            throw Feil(message = feilmelding)
        }
    }
}

@Service
@Profile("e2e", "integrasjonstest")
class E2EKafkaProducer : KafkaProducer {

    override fun sendHistorikkinnslag(behandlingId: UUID, key: String, request: OpprettHistorikkinnslagRequest) {
        logger.info("Skipper sending av historikkinnslag for behandling $behandlingId fordi kafka ikke er enablet")
    }

    override fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand) {
        logger.info("Skipper sending av saksstatistikk for behandling $behandlingId fordi kafka ikke er enablet")
    }

    override fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering) {
        logger.info("Skipper sending av vedtaksstatistikk for behandling $behandlingId fordi kafka ikke er enablet")
    }

    override fun sendHentFagsystemsbehandlingRequest(requestId: UUID, request: HentFagsystemsbehandlingRequest) {
        logger.info("Skipper sending av info-request for fagsystembehandling ${request.eksternId} fordi kafka ikke er enablet")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(E2EKafkaProducer::class.java)
    }
}
