package no.nav.familie.ba.sak.statistikk.producer

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.ekstern.pensjon.HentAlleIdenterTilPsysResponseDTO
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdBisysMelding
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface KafkaProducer {

    fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long
    fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long
    fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long

    fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    )

    fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding,
    )

    fun sendIdentTilPSys(
        hentAlleIdenterTilPsysResponseDTO: HentAlleIdenterTilPsysResponseDTO,
    )
}

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
@Primary
@Profile("!preprod-gcp & !prod-gcp")
class DefaultKafkaProducer(val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository) :
    KafkaProducer {

    private val vedtakV2Counter = Metrics.counter(COUNTER_NAME, "type", "vedtakV2")
    private val saksstatistikkSakDvhCounter = Metrics.counter(COUNTER_NAME, "type", "sak")
    private val saksstatistikkBehandlingDvhCounter = Metrics.counter(COUNTER_NAME, "type", "behandling")

    @Autowired
    @Qualifier("kafkaObjectMapper")
    lateinit var kafkaObjectMapper: ObjectMapper

    @Autowired
    lateinit var kafkaAivenTemplate: KafkaTemplate<String, String>

    override fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long {
        val vedtakForDVHV2Melding =
            kafkaObjectMapper.writeValueAsString(vedtakV2)
        val response = kafkaAivenTemplate.send(VEDTAKV2_TOPIC, vedtakV2.funksjonellId, vedtakForDVHV2Melding).get()
        logger.info("$VEDTAKV2_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        vedtakV2Counter.increment()
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long {
        val behandlingsMelding = kafkaObjectMapper.writeValueAsString(melding.jsonToBehandlingDVH())

        val response =
            kafkaAivenTemplate.send(SAKSSTATISTIKK_BEHANDLING_TOPIC, melding.funksjonellId, behandlingsMelding).get()
        logger.info("$SAKSSTATISTIKK_BEHANDLING_TOPIC -> message sent -> offset=${response.recordMetadata.offset()}")

        saksstatistikkBehandlingDvhCounter.increment()
        melding.offsetVerdi = response.recordMetadata.offset()
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long {
        val saksMelding = kafkaObjectMapper.writeValueAsString(melding.jsonToSakDVH())

        val response =
            kafkaAivenTemplate.send(SAKSSTATISTIKK_SAK_TOPIC, melding.funksjonellId, saksMelding).get()
        logger.info("$SAKSSTATISTIKK_SAK_TOPIC -> message sent -> offset=${response.recordMetadata.offset()}")

        saksstatistikkSakDvhCounter.increment()
        melding.offsetVerdi = response.recordMetadata.offset()
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        val meldingIString: String = objectMapper.writeValueAsString(melding)

        kafkaAivenTemplate.send(FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC, key, meldingIString)
            .thenAccept {
                logger.info(
                    "Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC for " +
                        "$behandlingId med $key er sendt. " +
                        "Fikk offset ${it?.recordMetadata?.offset()}",
                )
            }
            .exceptionally {
                val feilmelding =
                    "Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC kan ikke sendes for " +
                        "$behandlingId med $key. Feiler med ${it.message}"
                logger.warn(feilmelding)
                throw Feil(message = feilmelding)
            }
    }

    override fun sendIdentTilPSys(
        hentAlleIdenterTilPsysResponseDTO: HentAlleIdenterTilPsysResponseDTO,
    ) {
        kafkaAivenTemplate.send(BARNETRYGD_PENSJON_TOPIC, objectMapper.writeValueAsString(hentAlleIdenterTilPsysResponseDTO))
            .exceptionally {
                val feilmelding =
                    "Melding på topic $BARNETRYGD_PENSJON_TOPIC kan ikke sendes for " +
                        "RequestId: ${hentAlleIdenterTilPsysResponseDTO.requestId}. Feiler med ${it.message}"
                logger.warn(feilmelding)
                throw Feil(message = feilmelding)
            }
    }

    override fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding,
    ) {
        val opphørBarnetrygdBisysMelding =
            objectMapper.writeValueAsString(barnetrygdBisysMelding)

        kafkaAivenTemplate.send(OPPHOER_BARNETRYGD_BISYS_TOPIC, behandlingId, opphørBarnetrygdBisysMelding)
            .thenAccept {
                logger.info(
                    "Melding på topic $OPPHOER_BARNETRYGD_BISYS_TOPIC for " +
                        "$behandlingId er sendt. " +
                        "Fikk offset ${it?.recordMetadata?.offset()}",
                )
                secureLogger.info("Send barnetrygd bisys melding $opphørBarnetrygdBisysMelding")
            }
            .exceptionally {
                val feilmelding =
                    "Melding på topic $OPPHOER_BARNETRYGD_BISYS_TOPIC kan ikke sendes for " +
                        "$behandlingId. Feiler med ${it.message}"
                logger.warn(feilmelding)
                throw Feil(message = feilmelding)
            }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DefaultKafkaProducer::class.java)
        private const val VEDTAKV2_TOPIC = "teamfamilie.aapen-barnetrygd-vedtak-v2"
        private const val SAKSSTATISTIKK_BEHANDLING_TOPIC = "teamfamilie.aapen-barnetrygd-saksstatistikk-behandling-v1"
        private const val SAKSSTATISTIKK_SAK_TOPIC = "teamfamilie.aapen-barnetrygd-saksstatistikk-sak-v1"
        private const val COUNTER_NAME = "familie.ba.sak.kafka.produsert"
        private const val FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC =
            "teamfamilie.privat-tbk-hentfagsystemsbehandling-respons-topic"
        const val OPPHOER_BARNETRYGD_BISYS_TOPIC = "teamfamilie.aapen-familie-ba-sak-opphoer-barnetrygd"
        const val BARNETRYGD_PENSJON_TOPIC = "teamfamilie.aapen-familie-ba-sak-identer-med-barnetrygd"
    }
}

@Service
class MockKafkaProducer(val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository) :
    KafkaProducer {

    override fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long {
        logger.info("Skipper sending av vedtakV2 for ${vedtakV2.behandlingsId} fordi kafka Aiven for DVH V2 ikke er enablet")

        sendteMeldinger["vedtakV2-${vedtakV2.behandlingsId}"] = vedtakV2
        return 0
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long {
        logger.info("Skipper sending av saksstatistikk behandling for ${melding.jsonToBehandlingDVH().behandlingId} fordi kafka ikke er enablet")
        sendteMeldinger["behandling-${melding.jsonToBehandlingDVH().behandlingId}"] = melding.jsonToBehandlingDVH()
        melding.offsetVerdiOnPrem = 42
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return 42
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long {
        logger.info("Skipper sending av saksstatistikk sak for ${melding.jsonToSakDVH().sakId} fordi kafka ikke er enablet")
        sendteMeldinger["sak-${melding.jsonToSakDVH().sakId}"] = melding.jsonToSakDVH()
        melding.offsetVerdiOnPrem = 43
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return 43
    }

    override fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        logger.info("Skipper sending av fagsystemsbehandling respons for $behandlingId fordi kafka ikke er enablet")
    }
    override fun sendIdentTilPSys(
        hentAlleIdenterTilPsysResponseDTO: HentAlleIdenterTilPsysResponseDTO,
    ) {
        logger.info("Skipper sending av sendBarnetrygdBisysMelding respons for $hentAlleIdenterTilPsysResponseDTO.requestId fordi kafka ikke er enablet")
    }

    override fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding,
    ) {
        logger.info("Skipper sending av sendOpphørBarnetrygdBisys respons for $behandlingId fordi kafka ikke er enablet")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MockKafkaProducer::class.java)

        var sendteMeldinger = mutableMapOf<String, Any>()
    }
}
