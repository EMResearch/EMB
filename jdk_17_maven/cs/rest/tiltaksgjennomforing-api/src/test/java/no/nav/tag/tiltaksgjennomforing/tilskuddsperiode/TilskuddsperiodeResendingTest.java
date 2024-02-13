package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.FeatureToggleService;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
@SpringBootTest(properties = { "tiltaksgjennomforing.kafka.enabled=true" })
@DirtiesContext
@ActiveProfiles({ Miljø.LOCAL })
@EmbeddedKafka(partitions = 1, topics = { Topics.TILSKUDDSPERIODE_GODKJENT })
public class TilskuddsperiodeResendingTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;
    @Autowired
    private AvtaleRepository avtaleRepository;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Test
    public void sjekk_at_godkjent_med_samme_løpenummer_får_resendings_nummer() throws JSONException {
        when(featureToggleService.isEnabled(anyString())).thenReturn(true);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

        Now.fixedDate(LocalDate.of(2023, 03, 1));
        LocalDate avtaleStart = LocalDate.of(2022, 10, 20);
        LocalDate avtaleSlutt = LocalDate.of(2024, 3, 2);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleStart, avtaleSlutt);
        // Godkjenner første gang. Denne skal ikke ha noen resendingsnummer
        avtale.godkjennTilskuddsperiode(TestData.enNavIdent2(), "4321");
        avtale.nyeTilskuddsperioderEtterMigreringFraArena(LocalDate.of(2022, 10, 20), false);
        // Nå er perioden annullert en gang, godkjenner igjen. Da den nå har den samme løpenr må den få resendingsnummer = 1
        avtale.godkjennTilskuddsperiode(TestData.enNavIdent2(), "1234");
        avtale.getTilskuddPeriode().forEach(periode -> System.out.println(periode.getStartDato() + " " + periode.getLøpenummer() + " " + periode.getStatus()));
        avtaleRepository.save(avtale);

        //SÅ
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        records.records("Topics.TILSKUDDSPERIODE_GODKJENT").forEach(record -> {
            try {
                JSONObject jsonRefusjonRecord = new JSONObject(record.value());
                String enhet = (String)jsonRefusjonRecord.get("enhet");
                if("4321".equals(enhet)) {
                    assertThat(jsonRefusjonRecord.get("resendingsnummer")).isNull();
                }
                if("1234".equals(enhet)) {
                    assertThat((int)jsonRefusjonRecord.get("resendingsnummer")).isEqualTo(1);
                }
                assertThat(jsonRefusjonRecord.get("avtaleId")).isNotNull();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        Now.resetClock();

    }
}
