package no.nav.tag.tiltaksgjennomforing;

import lombok.AllArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@AllArgsConstructor
public class LokalSecurityAzureConfig {

    private final RestTemplateBuilder restTemplateBuilder;

    @Bean("notifikasjonerRestTemplate")
    public RestTemplate anonymProxyRestTemplate(){
        return restTemplateBuilder.build();
    }

    @Bean("veilarbarenaRestTemplate")
    public RestTemplate anonymProxyRestTemplateVeilabArena(){
        return restTemplateBuilder.build();
    }
}
