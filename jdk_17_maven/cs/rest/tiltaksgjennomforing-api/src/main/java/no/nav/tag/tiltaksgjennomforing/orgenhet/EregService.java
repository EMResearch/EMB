package no.nav.tag.tiltaksgjennomforing.orgenhet;

import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class EregService {
    private final EregProperties eregProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public Organisasjon hentVirksomhet(BedriftNr bedriftNr) {
        URI uri = UriComponentsBuilder.fromUri(eregProperties.getUri())
                .pathSegment(bedriftNr.asString())
                .build()
                .toUri();
        try {
            EregEnhet eregEnhet = restTemplate.getForObject(uri, EregEnhet.class);
            if ("JuridiskEnhet".equals(eregEnhet.getType())) {
                throw new EnhetErJuridiskException();
            } else if ("Organisasjonsledd".equals(eregEnhet.getType())) {
                throw new EnhetErOrganisasjonsleddException();
            }
            return eregEnhet.konverterTilDomeneObjekt();
        } catch (RestClientException e) {
            throw new EnhetFinnesIkkeException();
        }
    }
}
