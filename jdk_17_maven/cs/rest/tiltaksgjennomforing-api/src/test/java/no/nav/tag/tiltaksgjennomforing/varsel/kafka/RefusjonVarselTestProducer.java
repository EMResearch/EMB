package no.nav.tag.tiltaksgjennomforing.varsel.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
@Component
@Slf4j
public class RefusjonVarselTestProducer {
    KafkaTemplate<String, RefusjonVarselMelding> kafkaTemplate;

    RefusjonVarselTestProducer(@Qualifier("refusjonVarselTestMeldingKafkaTemplate") KafkaTemplate<String, RefusjonVarselMelding> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publiserMelding(String meldingId, RefusjonVarselMelding refusjonVarselMelding) {
        kafkaTemplate.send(Topics.TILTAK_VARSEL, meldingId, refusjonVarselMelding)
                .addCallback(new ListenableFutureCallback<>() {
                    @Override
                    public void onSuccess(SendResult<String, RefusjonVarselMelding> result) {
                        log.info("Melding med id {} sendt til Kafka topic {}", meldingId, Topics.TILTAK_VARSEL);
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("Melding med id {} kunne ikke sendes til Kafka topic {}", meldingId, Topics.TILTAK_VARSEL);
                        log.error("Feilmelding: ", ex);
                    }
                });
    }

}
