package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeAnnullert;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeForkortet;
import no.nav.tag.tiltaksgjennomforing.avtale.events.TilskuddsperiodeGodkjent;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
@Component
@Slf4j
public class TilskuddsperiodeKafkaProducer {

    private final KafkaTemplate<String, String> aivenKafkaTemplate;
    private final ObjectMapper objectMapper;

    public TilskuddsperiodeKafkaProducer(@Qualifier("aivenKafkaTemplate") KafkaTemplate<String, String> aivenKafkaTemplate, ObjectMapper objectMapper) {
        this.aivenKafkaTemplate = aivenKafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener
    public void tilskuddsperiodeGodkjent(TilskuddsperiodeGodkjent event) {
        TilskuddsperiodeGodkjentMelding melding = TilskuddsperiodeGodkjentMelding.create(event.getAvtale(), event.getTilskuddsperiode(), event.getResendingsnummer());
        publiserTilskuddsperiodeGodkjentMelding(melding);
    }

    @TransactionalEventListener
    public void tilskuddsperiodeAnnullert(TilskuddsperiodeAnnullert event) {
        UUID tilskuddsperiodeId = event.getTilskuddsperiode().getId();
        TilskuddsperiodeAnnullertMelding melding = new TilskuddsperiodeAnnullertMelding(tilskuddsperiodeId, TilskuddsperiodeAnnullertÅrsak.AVTALE_ANNULLERT);
        publiserTilskuddsperiodeAnnullertMelding(melding);
    }

    @TransactionalEventListener
    public void tilskuddsperiodeForkortet(TilskuddsperiodeForkortet event) {
        UUID tilskuddsperiodeId = event.getTilskuddsperiode().getId();
        TilskuddsperiodeForkortetMelding melding = new TilskuddsperiodeForkortetMelding(tilskuddsperiodeId, event.getTilskuddsperiode().getBeløp(), event.getTilskuddsperiode().getSluttDato());
        publiserTilskuddsperiodeForkortetMelding(melding);
    }

    public void publiserTilskuddsperiodeGodkjentMelding(TilskuddsperiodeGodkjentMelding melding) {
        publiserMelding(Topics.TILSKUDDSPERIODE_GODKJENT, melding.getTilskuddsperiodeId().toString(), melding);
    }

    public void publiserTilskuddsperiodeAnnullertMelding(TilskuddsperiodeAnnullertMelding melding) {
        publiserMelding(Topics.TILSKUDDSPERIODE_ANNULLERT, melding.getTilskuddsperiodeId().toString(), melding);
    }

    public void publiserTilskuddsperiodeForkortetMelding(TilskuddsperiodeForkortetMelding melding) {
        publiserMelding(Topics.TILSKUDDSPERIODE_FORKORTET, melding.getTilskuddsperiodeId().toString(), melding);
    }

    private void publiserMelding(String topic, String meldingId, Object melding) {
        String meldingSomString;
        try {
            meldingSomString = objectMapper.writeValueAsString(melding);
        } catch (JsonProcessingException e) {
            log.error("Kunne ikke lage JSON for melding med id {} til topic {}", meldingId, topic);
            return;
        }

        aivenKafkaTemplate.send(topic, meldingId, meldingSomString)
                .addCallback(new ListenableFutureCallback<>() {
                    @Override
                    public void onSuccess(SendResult<String, String> result) {
                        log.info("Melding med id {} sendt til Kafka topic {}", meldingId, topic);
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.warn("Melding med id {} kunne ikke sendes til Kafka topic {}", meldingId, topic);
                    }
                });
    }
}
