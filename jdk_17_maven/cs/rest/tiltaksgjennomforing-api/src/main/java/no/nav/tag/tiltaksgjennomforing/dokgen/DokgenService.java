package no.nav.tag.tiltaksgjennomforing.dokgen;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.journalfoering.AvtaleTilJournalfoeringMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DokgenService {
    private static final BigDecimal HUNDRE = new BigDecimal("100");

    private final DokgenProperties dokgenProperties;
    private final MeterRegistry meterRegistry;

    public byte[] avtalePdf(Avtale avtale, Avtalerolle avtalerolle) {
        var avtaleTilJournalfoering = AvtaleTilJournalfoeringMapper.tilJournalfoering(avtale.getGjeldendeInnhold(), avtalerolle);
        gangOppSatserMed100(avtaleTilJournalfoering);
        fjernGodkjentPåVegneAv(avtaleTilJournalfoering);
        try {
            byte[] bytes = restOperations().postForObject(dokgenProperties.getUri(), avtaleTilJournalfoering, byte[].class);
            meterRegistry.counter("tiltaksgjennomforing.pdf.ok").increment();
            return bytes;
        } catch (RestClientException e) {
            log.error("Feil ved kall til dokgen for henting av PDF", e);
            meterRegistry.counter("tiltaksgjennomforing.pdf.feil").increment();
            throw e;
        }
    }

    // TODO: Det bør heller ganges med 100 fra starten av i AvtaleTilJournalfoering.
    //  Slik som det er nå så gjøres det ganging med 100 både her og i tiltaksgjennomforing-prosess.
    //  Endring på dette krever en synkronisert fiks både her og i tiltaksgjennomforing-prosess.
    private void gangOppSatserMed100(no.nav.tag.tiltaksgjennomforing.journalfoering.AvtaleTilJournalfoering avtaleTilJournalfoering) {
        if (avtaleTilJournalfoering.getArbeidsgiveravgift() != null) {
            avtaleTilJournalfoering.setArbeidsgiveravgift(avtaleTilJournalfoering.getArbeidsgiveravgift().multiply(HUNDRE));
        }
        if (avtaleTilJournalfoering.getFeriepengesats() != null) {
            avtaleTilJournalfoering.setFeriepengesats(avtaleTilJournalfoering.getFeriepengesats().multiply(HUNDRE));
        }
        if (avtaleTilJournalfoering.getOtpSats() != null) {
            avtaleTilJournalfoering.setOtpSats(avtaleTilJournalfoering.getOtpSats() * 100);
        }
    }

    private void fjernGodkjentPåVegneAv(no.nav.tag.tiltaksgjennomforing.journalfoering.AvtaleTilJournalfoering avtaleTilJournalfoering) {
        avtaleTilJournalfoering.setGodkjentPaVegneAv(false);
        avtaleTilJournalfoering.setGodkjentPaVegneGrunn(null);
    }

    // Lager ny instans av RestOperations i stedet for å wire inn RestTemplate fordi det var vanskelig å få den til å bruke en ObjectMapper som hadde datoer på format 'yyyy-MM-dd' i stedet for et array
    private RestOperations restOperations() {
        RestTemplate rest = new RestTemplate();
        //this is crucial!
        rest.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());
        return rest;
    }

    private MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
        var converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    private ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
