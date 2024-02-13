package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.sts.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersondataService {
    private final RestTemplate restTemplate;
    private final STSClient stsClient;
    private final PersondataProperties persondataProperties;

    @Value("classpath:pdl/hentPerson.adressebeskyttelse.graphql")
    private Resource adressebeskyttelseQueryResource;

    @Value("classpath:pdl/hentPersondata.graphql")
    private Resource persondataQueryResource;

    @Value("classpath:pdl/hentIdenter.graphql")
    private Resource identerQueryResource;

    @SneakyThrows
    private static String resourceAsString(Resource adressebeskyttelseQuery) {
        String filinnhold = StreamUtils.copyToString(adressebeskyttelseQuery.getInputStream(), StandardCharsets.UTF_8);
        return filinnhold.replaceAll("\\s+", " ");
    }

    protected Adressebeskyttelse hentAdressebeskyttelse(Fnr fnr) {
        PdlRequest pdlRequest = new PdlRequest(resourceAsString(adressebeskyttelseQueryResource), new Variables(fnr.asString()));
        return hentAdressebeskyttelseFraPdlRespons(utførKallTilPdl(pdlRequest));
    }

    private HttpEntity<String> createRequestEntity(PdlRequest pdlRequest) {
        String stsToken = stsClient.hentSTSToken().getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(stsToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tema", "GEN");
        headers.set("Nav-Consumer-Token", "Bearer " + stsToken);
        return new HttpEntity(pdlRequest, headers);
    }

    private static Adressebeskyttelse hentAdressebeskyttelseFraPdlRespons(PdlRespons pdlRespons) {
        try {
            return pdlRespons.getData().getHentPerson().getAdressebeskyttelse()[0];
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            return Adressebeskyttelse.INGEN_BESKYTTELSE;
        }
    }

    public static Navn hentNavnFraPdlRespons(PdlRespons pdlRespons) {
        try {
            return pdlRespons.getData().getHentPerson().getNavn()[0];
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            return Navn.TOMT_NAVN;
        }
    }

    private static String hentAktørIdFraPdlRespons(PdlRespons pdlRespons) {
        try {
            return pdlRespons.getData().getHentIdenter().getIdenter()[0].getIdent();
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    public static Optional<String> hentGeoLokasjonFraPdlRespons(PdlRespons pdlRespons) {
        try {
            return Optional.of(pdlRespons.getData().getHentGeografiskTilknytning().getGeoTilknytning());
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private PdlRespons utførKallTilPdl(PdlRequest pdlRequest) {
        try {
            return restTemplate.postForObject(persondataProperties.getUri(), createRequestEntity(pdlRequest), PdlRespons.class);
        } catch (RestClientException exception) {
            log.error("Feil fra PDL med request-url: " + persondataProperties.getUri(), exception);
            throw exception;
        }
    }

    public String hentAktørId(Fnr fnr) {
        PdlRequest pdlRequest = new PdlRequest(resourceAsString(identerQueryResource), new Variables(fnr.asString()));
        return hentAktørIdFraPdlRespons(utførKallTilPdl(pdlRequest));
    }

    public boolean erKode6Eller7(Fnr fnr) {
        String gradering = hentAdressebeskyttelse(fnr).getGradering();
        return "FORTROLIG".equals(gradering) || "STRENGT_FORTROLIG".equals(gradering) || "STRENGT_FORTROLIG_UTLAND".equals(gradering);
    }

    public boolean erKode6(PdlRespons pdlRespons) {
        try {
            String gradering = hentAdressebeskyttelseFraPdlRespons(pdlRespons).getGradering();
            return "STRENGT_FORTROLIG".equals(gradering) || "STRENGT_FORTROLIG_UTLAND".equals(gradering);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean erKode6(Fnr fnr) {
        String gradering = hentAdressebeskyttelse(fnr).getGradering();
        return "STRENGT_FORTROLIG".equals(gradering) || "STRENGT_FORTROLIG_UTLAND".equals(gradering);
    }

    @Cacheable(EhCacheConfig.PDL_CACHE)
    public PdlRespons hentPersondataFraPdl(Fnr fnr) {
        PdlRequest pdlRequest = new PdlRequest(resourceAsString(persondataQueryResource), new Variables(fnr.asString()));
        return utførKallTilPdl(pdlRequest);
    }

    public PdlRespons hentPersondata(Fnr fnr) {
        PdlRequest pdlRequest = new PdlRequest(resourceAsString(persondataQueryResource), new Variables(fnr.asString()));
        return utførKallTilPdl(pdlRequest);
    }
}
