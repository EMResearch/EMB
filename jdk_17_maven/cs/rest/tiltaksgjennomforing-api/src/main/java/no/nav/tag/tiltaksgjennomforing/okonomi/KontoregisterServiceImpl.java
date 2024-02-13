package no.nav.tag.tiltaksgjennomforing.okonomi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.exceptions.KontoregisterFantIkkeBedriftFeilException;
import no.nav.tag.tiltaksgjennomforing.exceptions.KontoregisterFeilException;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.CorrelationIdSupplier;
import no.nav.tag.tiltaksgjennomforing.utils.ConditionalOnPropertyNotEmpty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("tiltaksgjennomforing.kontoregister.realClient")
public class KontoregisterServiceImpl implements KontoregisterService{

    private final KontoregisterProperties kontoregisterProperties;
    private final RestTemplate restTemplate;

    public KontoregisterServiceImpl(KontoregisterProperties kontoregisterProperties, @Qualifier("azure") RestTemplate restTemplate) {
        this.kontoregisterProperties = kontoregisterProperties;
        this.restTemplate = restTemplate;
    }

    public String hentKontonummer(String bedriftNr)  {
        try {
            ResponseEntity<KontoregisterResponse> response = restTemplate.exchange(new URI(String.format("%s/%s", kontoregisterProperties.getUri(),bedriftNr)),HttpMethod.GET,lagRequest(),  KontoregisterResponse.class);
            return response.getBody().getKontonr();

        } catch (RestClientException | URISyntaxException  exception) {
            if(exception instanceof HttpClientErrorException){
                HttpClientErrorException hcee = (HttpClientErrorException)exception;
                if(hcee.getStatusCode() == NOT_FOUND) {
                    throw new KontoregisterFantIkkeBedriftFeilException();
                }

            }
            log.error(String.format("Feil fra kontoregister med request-url: %s/%s", kontoregisterProperties.getUri(),bedriftNr), exception);
            throw new KontoregisterFeilException();
        }
    }

    private HttpEntity lagRequest(){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Nav-consumer-Id", kontoregisterProperties.getConsumerId());
        headers.set("Nav-Call-Id", CorrelationIdSupplier.get());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity(headers);
    }
}
