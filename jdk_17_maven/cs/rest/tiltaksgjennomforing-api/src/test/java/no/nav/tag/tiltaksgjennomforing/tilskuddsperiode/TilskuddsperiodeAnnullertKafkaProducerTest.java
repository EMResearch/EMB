package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.FeatureToggleService;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = { "tiltaksgjennomforing.kafka.enabled=true" })
@DirtiesContext
@ActiveProfiles({ Miljø.LOCAL })
@EmbeddedKafka(partitions = 1, topics = { Topics.TILSKUDDSPERIODE_ANNULLERT })
class TilskuddsperiodeAnnullertKafkaProducerTest {

    @Autowired
    private TilskuddsperiodeKafkaProducer tilskuddsperiodeKafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Test
    public void skal_kunne_sende_tilskuddperiode_annullert_på_kafka_topic() throws JSONException {
        when(featureToggleService.isEnabled(anyString())).thenReturn(true);

        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

        // GITT
        final UUID tilskuddPeriodeId = UUID.randomUUID();
        var tilskuddMelding = new TilskuddsperiodeAnnullertMelding(tilskuddPeriodeId, TilskuddsperiodeAnnullertÅrsak.AVTALE_ANNULLERT);

        //NÅR
        tilskuddsperiodeKafkaProducer.publiserTilskuddsperiodeAnnullertMelding(tilskuddMelding);

        //SÅ
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, Topics.TILSKUDDSPERIODE_ANNULLERT);
        JSONObject jsonRefusjonRecord = new JSONObject(record.value());
        assertThat(jsonRefusjonRecord.get("tilskuddsperiodeId")).isEqualTo(tilskuddPeriodeId.toString());
    }
}