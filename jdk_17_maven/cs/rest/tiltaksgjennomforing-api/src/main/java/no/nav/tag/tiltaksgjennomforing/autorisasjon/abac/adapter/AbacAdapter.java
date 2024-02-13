package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter;

import no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.sts.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter.AbacTransformer.tilAbacRequestBody;

@Service
public class AbacAdapter {
    final Logger log = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate;

    private final STSClient stsClient;

    private final AbacProperties abacProperties;

    private final Cache cache;

    public AbacAdapter(RestTemplate restTemplate, STSClient stsClient, AbacProperties abacProperties, EhCacheCacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.stsClient = stsClient;
        this.abacProperties = abacProperties;
        this.cache = cacheManager.getCache(EhCacheConfig.ABAC_CACHE);
    }

    Map<String, String> cacheKey(String navIdent, String deltakerFnr) {
        return Map.of("navIdent", navIdent, "deltakerFnr", deltakerFnr);
    }

    private AbacResponse hentRespons(String navIdent, String deltakerFnr) {
        return restTemplate.postForObject(
                abacProperties.getUri(),
                getHttpEntity(tilAbacRequestBody(navIdent, deltakerFnr)),
                AbacResponse.class
        );
    }

    public boolean harSkriveTilgang(String navIdent, String deltakerFnr) {
        if (navIdent == null) {
            log.error("Navident manglet i tilgangskontroll");
            return false;
        }
        if (deltakerFnr == null) {
            log.error("DeltakerFnr manglet i tilgangskontroll");
            return false;
        }
        var key = cacheKey(navIdent, deltakerFnr);
        var cachedValue = cache.get(key, Boolean.class);
        if (cachedValue != null) {
            return cachedValue;
        }
        try {
            var result = Objects.equals(hentRespons(navIdent, deltakerFnr).response.decision, "Permit");
            cache.putIfAbsent(key, result);
            return result;
        } catch (RuntimeException ex) {
            log.error("Abac feil", ex);
            return false;
        }
    }

    private HttpEntity getHttpEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Nav-Consumer-Id", abacProperties.getNavConsumerId());
        headers.set("Nav-Call-Id", UUID.randomUUID().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(stsClient.hentSTSToken().getAccessToken());
        return new HttpEntity<>(body, headers);
    }

    @CacheEvict(cacheNames = EhCacheConfig.ABAC_CACHE, allEntries = true)
    public void cacheEvict() {
        log.info("Tømmer abac cache for data");
    }

}
