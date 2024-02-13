package no.nav.tag.tiltaksgjennomforing.varsel.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.varsel.Sms;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
@Component
@Slf4j
public class SmsProducer {
    private final KafkaTemplate<String, Sms> kafkaTemplate;

    public SmsProducer(@Qualifier("aivenTiltaksgjennomforingVarsel") KafkaTemplate<String, Sms> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendSmsVarselMeldingTilKafka(Sms sms) {
        kafkaTemplate.send(Topics.TILTAK_SMS, sms.getSmsVarselId().toString(), sms).addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable ex) {
                log.warn("Sms med id={} kunne ikke sendes til Kafka topic", sms.getSmsVarselId());
            }

            @Override
            public void onSuccess(SendResult<String, Sms> result) {
                log.info("Sms med id={} sendt på Kafka topic {}", sms.getSmsVarselId(), result.getProducerRecord().topic());
            }
        });
    }
}
