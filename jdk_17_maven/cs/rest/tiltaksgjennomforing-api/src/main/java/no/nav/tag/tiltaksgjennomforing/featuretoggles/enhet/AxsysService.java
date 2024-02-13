package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.CorrelationIdSupplier;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
@Slf4j
public class AxsysService {
    private final AxsysProperties axsysProperties;
    private final RestTemplate restTemplate;

    public AxsysService(AxsysProperties axsysProperties, RestTemplate restTemplate) {
        this.axsysProperties = axsysProperties;
        this.restTemplate = restTemplate;
    }

    @Cacheable(EhCacheConfig.AXSYS_CACHE)
    public List<NavEnhet> hentEnheterNavAnsattHarTilgangTil(NavIdent ident) {
        URI uri = UriComponentsBuilder.fromUri(axsysProperties.getUri())
                .pathSegment(ident.asString())
                .queryParam("inkluderAlleEnheter", "false")
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Nav-Call-Id", CorrelationIdSupplier.get());
        headers.set("Nav-Consumer-Id", axsysProperties.getNavConsumerId());

        try {
            AxsysRespons respons = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), AxsysRespons.class).getBody();
            return respons.tilEnheter();
        } catch (RestClientException exception) {
            log.warn("Feil ved henting av enheter for ident " + ident, exception);
            throw exception;
        }
    }

    @CacheEvict(cacheNames= EhCacheConfig.AXSYS_CACHE, allEntries=true)
    public void cacheEvict() {
        log.info("Tømmer axsys cache for data");
    }

}