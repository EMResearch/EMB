package no.nav.tag.tiltaksgjennomforing.varsel.oppgave;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.exceptions.GosysFeilException;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.sts.STSClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
public class OppgaveVarselService {

    private static final String CORR_ID = "X-Correlation-ID";
    private final URI uri;
    private final RestTemplate restTemplate;
    private final STSClient stsClient;

    public OppgaveVarselService(OppgaveProperties props, RestTemplate restTemplate, STSClient stsClient) {
        uri = UriComponentsBuilder.fromUri(props.getOppgaveUri()).build().toUri();
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
    }

    public void opprettOppgave(String aktørId, Tiltakstype tiltakstype, UUID avtaleId) {
        OppgaveRequest oppgaveRequest = new OppgaveRequest(aktørId, tiltakstype);
        OppgaveResponse oppgaveResponse;

            try {
                oppgaveResponse = restTemplate.postForObject(uri, entityMedStsToken(oppgaveRequest, avtaleId), OppgaveResponse.class);
            } catch (Exception e2) {
                log.error("Kall til Oppgave feilet, avtaleId={} : {}", avtaleId, e2.getMessage());
                throw new GosysFeilException();
            }
        log.info("Opprettet oppgave for tiltak {}. OppgaveId={}, avtaleId={}", tiltakstype.getBeskrivelse(), oppgaveResponse.getId(), avtaleId);
    }

    private HttpEntity<OppgaveRequest> entityMedStsToken(final OppgaveRequest oppgaveRequest, UUID correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(stsClient.hentSTSToken().getAccessToken());
        headers.set(CORR_ID, correlationId.toString());
        HttpEntity<OppgaveRequest> entity = new HttpEntity<>(oppgaveRequest, headers);
        return entity;
    }

    @Data
    static class OppgaveResponse{
        private String id;
    }
}
