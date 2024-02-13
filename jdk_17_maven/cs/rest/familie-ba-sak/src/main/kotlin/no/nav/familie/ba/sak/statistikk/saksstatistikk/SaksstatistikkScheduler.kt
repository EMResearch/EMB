package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SaksstatistikkScheduler(
    val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    val kafkaProducer: KafkaProducer,
) {

    @Scheduled(fixedDelay = 60000)
    fun sendKafkameldinger() {
        if (LeaderClient.isLeader() == true) {
            sendSaksstatistikk()
        }
    }

    @Transactional
    fun sendSaksstatistikk() {
        val meldinger = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()

        for (melding in meldinger) {
            try {
                when (melding.type) {
                    SaksstatistikkMellomlagringType.SAK -> {
                        kafkaProducer.sendMessageForTopicSak(melding)
                    }

                    SaksstatistikkMellomlagringType.BEHANDLING -> {
                        kafkaProducer.sendMessageForTopicBehandling(melding)
                    }
                }
            } catch (e: Exception) {
                logger.error("Kunne ikke sende melding med ${melding.id},type ${melding.type} og fagsakId/behandlingid=${melding.typeId} til kafka")
                secureLogger.error("Kunne ikke sende melding med ${melding.id},type ${melding.type} og fagsakId/behandlingid=${melding.typeId} til kafka. $melding", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SaksstatistikkScheduler::class.java)
    }
}
