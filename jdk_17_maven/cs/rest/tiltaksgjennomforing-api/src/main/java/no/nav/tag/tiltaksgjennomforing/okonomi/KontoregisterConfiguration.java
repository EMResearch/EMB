package no.nav.tag.tiltaksgjennomforing.okonomi;

import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import no.nav.tag.tiltaksgjennomforing.utils.ConditionalOnPropertyNotEmpty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
@ConditionalOnPropertyNotEmpty("tiltaksgjennomforing.kontoregister.azureConfig")
class KontoregisterConfiguration {

    /*
     * Create one RestTemplate per OAuth2 client entry to separate between different scopes per API
     */
    @Bean("azure")
    RestTemplate downstreamResourceRestTemplate(RestTemplateBuilder restTemplateBuilder,
                                         ClientConfigurationProperties clientConfigurationProperties,
                                         OAuth2AccessTokenService oAuth2AccessTokenService) {

        ClientProperties clientProperties =
                Optional.ofNullable(clientConfigurationProperties.getRegistration().get("kontoregister"))
                        .orElseThrow(() -> new RuntimeException("could not find oauth2 client config for kontoregister"));
        return restTemplateBuilder
                .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
                .build();
    }


    private ClientHttpRequestInterceptor bearerTokenInterceptor(ClientProperties clientProperties,
                                                                OAuth2AccessTokenService oAuth2AccessTokenService) {
        return (request, body, execution) -> {
            OAuth2AccessTokenResponse response =
                    oAuth2AccessTokenService.getAccessToken(clientProperties);
            request.getHeaders().setBearerAuth(response.getAccessToken());
            return execution.execute(request, body);
        };
    }

}