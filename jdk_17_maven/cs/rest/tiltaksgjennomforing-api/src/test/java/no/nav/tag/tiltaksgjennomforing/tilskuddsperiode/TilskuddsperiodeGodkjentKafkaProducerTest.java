package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.FeatureToggleService;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = { "tiltaksgjennomforing.kafka.enabled=true" })
@DirtiesContext
@ActiveProfiles({ Miljø.LOCAL })
@EmbeddedKafka(partitions = 1, topics = { Topics.TILSKUDDSPERIODE_GODKJENT })
class TilskuddsperiodeGodkjentKafkaProducerTest {

    @Autowired
    private TilskuddsperiodeKafkaProducer tilskuddsperiodeKafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Test
    public void skal_kunne_sende_tilskuddperiode_godkjent_på_kafka_topic() throws JSONException {
        when(featureToggleService.isEnabled(anyString())).thenReturn(true);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

        // GITT
        final UUID avtaleId = UUID.randomUUID();
        final UUID tilskuddPeriodeId = UUID.randomUUID();
        final UUID avtaleInnholdId = UUID.randomUUID();
        final LocalDate avtaleFom = LocalDate.of(2023, 1, 1);
        final LocalDate avtaleTom = LocalDate.of(2023, 5, 1);
        final Tiltakstype tiltakstype = Tiltakstype.VARIG_LONNSTILSKUDD;
        final String deltakerFornavn = "Donald";
        final String deltakerEtternavn = "Duck";
        final Identifikator deltakerFnr = new Fnr("12345678901");
        final String arbeidsgiverFornavn = "Arne";
        final String arbeidsgiverEtternavn = "Arbeidsgiver";
        final String arbeidsgiverTlf = "41111111";
        final NavIdent veilederNavIdent = new NavIdent("X123456");
        final String bedriftNavn = "Donald Delivery";
        final BedriftNr bedriftnummer = new BedriftNr("99999999");
        final Integer tilskuddBeløp = 12000;
        final LocalDate tilskuddFraDato = Now.localDate().minusDays(15);
        final LocalDate tilskuddTilDato = Now.localDate().plusMonths(2);
        final Integer avtaleNr = 234234234;
        final Integer løpenummer = 3;
        final NavIdent beslutterNavIdent = new NavIdent("X234567");

        final TilskuddsperiodeGodkjentMelding tilskuddMelding = new TilskuddsperiodeGodkjentMelding(avtaleId,
                tilskuddPeriodeId, avtaleInnholdId, avtaleFom, avtaleTom, tiltakstype, deltakerFornavn, deltakerEtternavn,
                deltakerFnr, arbeidsgiverFornavn, arbeidsgiverEtternavn, arbeidsgiverTlf, veilederNavIdent, bedriftNavn, bedriftnummer, tilskuddBeløp, tilskuddFraDato, tilskuddTilDato, 10.6, 0.02, 14.1, 60, avtaleNr, løpenummer, 0,
            "4808", beslutterNavIdent, LocalDateTime.now());

        //NÅR
        tilskuddsperiodeKafkaProducer.publiserTilskuddsperiodeGodkjentMelding(tilskuddMelding);

        //SÅ
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, Topics.TILSKUDDSPERIODE_GODKJENT);
        JSONObject jsonRefusjonRecord = new JSONObject(record.value());
        assertThat(jsonRefusjonRecord.get("avtaleId")).isNotNull();
        assertThat(jsonRefusjonRecord.get("tilskuddsperiodeId")).isNotNull();
        assertThat(jsonRefusjonRecord.get("avtaleInnholdId")).isNotNull();
        assertThat(jsonRefusjonRecord.get("tiltakstype")).isNotNull();
        assertThat(jsonRefusjonRecord.get("deltakerFornavn")).isNotNull();
        assertThat(jsonRefusjonRecord.get("deltakerEtternavn")).isNotNull();
        assertThat(jsonRefusjonRecord.get("deltakerFnr")).isNotNull();
        assertThat(jsonRefusjonRecord.get("arbeidsgiverFornavn")).isNotNull();
        assertThat(jsonRefusjonRecord.get("arbeidsgiverEtternavn")).isNotNull();
        assertThat(jsonRefusjonRecord.get("arbeidsgiverTlf")).isNotNull();
        assertThat(jsonRefusjonRecord.get("veilederNavIdent")).isNotNull();
        assertThat(jsonRefusjonRecord.get("bedriftNavn")).isNotNull();
        assertThat(jsonRefusjonRecord.get("bedriftNr")).isNotNull();
        assertThat(jsonRefusjonRecord.get("tilskuddsbeløp")).isNotNull();
        assertThat(jsonRefusjonRecord.get("tilskuddFom")).isNotNull().isOfAnyClassIn(String.class);
        assertThat(jsonRefusjonRecord.get("tilskuddTom")).isNotNull().isOfAnyClassIn(String.class);
        assertThat(jsonRefusjonRecord.get("feriepengerSats")).isNotNull();
        assertThat(jsonRefusjonRecord.get("otpSats")).isNotNull();
        assertThat(jsonRefusjonRecord.get("arbeidsgiveravgiftSats")).isNotNull();
        assertThat(jsonRefusjonRecord.get("lønnstilskuddsprosent")).isNotNull();
        assertThat(jsonRefusjonRecord.get("avtaleNr")).isNotNull();
        assertThat(jsonRefusjonRecord.get("løpenummer")).isNotNull();
        assertThat(jsonRefusjonRecord.get("beslutterNavIdent")).isNotNull();
        assertThat(jsonRefusjonRecord.get("godkjentTidspunkt")).isNotNull();
    }

}
