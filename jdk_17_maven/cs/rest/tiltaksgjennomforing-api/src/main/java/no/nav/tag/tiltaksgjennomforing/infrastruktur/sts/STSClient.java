package no.nav.tag.tiltaksgjennomforing.infrastruktur.sts;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class STSClient {

    private final RestTemplate stsBasicAuthRestTemplate;
    private final URI stsUri;
    public STSClient(StsProperties stsProperties) {
        this.stsBasicAuthRestTemplate = new RestTemplateBuilder()
                .basicAuthentication(stsProperties.getUsername(), stsProperties.getPassword())
                .build();
        this.stsUri = stsProperties.getRestUri();
    }
    public STSToken hentSTSToken() {
        String uriString = UriComponentsBuilder.fromUri(stsUri)
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .toUriString();

        return stsBasicAuthRestTemplate.exchange(
                uriString,
                HttpMethod.GET,
                getRequestEntity(),
                STSToken.class
        ).getBody();

    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(headers);
    }

}
