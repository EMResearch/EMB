package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;

@RequiredArgsConstructor
public class HentArbeidsgiverTokenxImpl implements HentArbeidsgiverToken {
    private final OAuth2AccessTokenService oAuth2AccessTokenService;
    private final ClientConfigurationProperties clientConfigurationProperties;

    @Override
    public String hentArbeidsgiverToken() {
        ClientProperties clientProperties = clientConfigurationProperties.getRegistration().get("tokenx-altinn");
        String accessToken = oAuth2AccessTokenService.getAccessToken(clientProperties).getAccessToken();
        return accessToken;
    }
}
