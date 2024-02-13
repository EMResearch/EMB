package no.nav.tag.tiltaksgjennomforing.varsel.kafka;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = { "tiltaksgjennomforing.kafka.enabled=true" })
@DirtiesContext
@ActiveProfiles({ Miljø.LOCAL })
@EnableKafka
@EmbeddedKafka(partitions = 1, topics = { Topics.TILTAK_VARSEL })

class RefusjonVarselConsumerTest {

    @Autowired
    RefusjonVarselTestProducer refusjonVarselTestProducer;

    @Autowired
    private AvtaleRepository avtaleRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

     @Test
     public void skal_sende_sms_når_det_leses_varsel_kafkamelding() throws InterruptedException {
         Now.fixedDate(LocalDate.of(2021, 6, 1));
         Avtale avtale = TestData.enSommerjobbAvtaleGodkjentAvBeslutter();
         avtale = avtaleRepository.save(avtale);
         LocalDate fristForGodkjenning = avtale.tilskuddsperiode(0).getSluttDato().plusMonths(2);
         var varselMelding = new RefusjonVarselMelding(
                 avtale.getId(),
                 avtale.tilskuddsperiode(0).getId(),
                 VarselType.KLAR,
                 fristForGodkjenning
         );
         refusjonVarselTestProducer.publiserMelding("testId-KLAR", varselMelding);
         Thread.sleep(1000L);


         Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", this.embeddedKafkaBroker);
         consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
         ConsumerFactory<String, RefusjonVarselMelding> consumerFactory = new DefaultKafkaConsumerFactory<>(
                 consumerProps,
                 new StringDeserializer(),
                 new JsonDeserializer<>(RefusjonVarselMelding.class)
         );
         Consumer<String, RefusjonVarselMelding> consumer = consumerFactory.createConsumer();
         this.embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, Topics.TILTAK_VARSEL);
         ConsumerRecords<String, RefusjonVarselMelding> replies = KafkaTestUtils.getRecords(consumer);
         assertThat(replies.count()).isGreaterThanOrEqualTo(1);
         Now.resetClock();
     }

}