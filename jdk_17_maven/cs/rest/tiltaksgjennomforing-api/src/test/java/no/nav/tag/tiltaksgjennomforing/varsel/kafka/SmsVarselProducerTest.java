package no.nav.tag.tiltaksgjennomforing.varsel.kafka;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.varsel.Sms;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(properties = { "tiltaksgjennomforing.kafka.enabled=true" })
@ActiveProfiles({ Miljø.LOCAL })
@EmbeddedKafka(partitions = 1, controlledShutdown = false, topics = { Topics.TILTAK_SMS })
public class SmsVarselProducerTest {
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;
    @Autowired
    private SmsProducer producer;

    private Consumer<String, String> consumer;

    @BeforeEach
    public void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, Topics.TILTAK_SMS);
    }

    @Test
    public void smsVarselOpprettet__skal_sendes_på_kafka_topic_med_riktige_felter() throws JSONException {
        producer.sendSmsVarselMeldingTilKafka(Sms.nyttVarsel("tlf", new Identifikator("id"), "melding", HendelseType.AVTALE_INNGÅTT, UUID.randomUUID()));

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, Topics.TILTAK_SMS);
        JSONObject json = new JSONObject(record.value());
        assertThat(json.getString("smsVarselId")).isNotNull();
        assertThat(json.getString("identifikator")).isEqualTo("id");
        assertThat(json.getString("meldingstekst")).isEqualTo("melding");
        assertThat(json.getString("telefonnummer")).isEqualTo("tlf");
    }
}
