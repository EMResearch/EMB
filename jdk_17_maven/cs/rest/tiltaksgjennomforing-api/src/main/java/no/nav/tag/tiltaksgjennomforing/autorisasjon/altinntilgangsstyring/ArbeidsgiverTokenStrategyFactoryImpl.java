package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils.Issuer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Profile(value = { Miljø.PROD_FSS, Miljø.DEV_FSS })
public class ArbeidsgiverTokenStrategyFactoryImpl implements ArbeidsgiverTokenStrategyFactory {
    private final TokenUtils tokenUtils;
    private final OAuth2AccessTokenService oAuth2AccessTokenService;
    private final ClientConfigurationProperties clientConfigurationProperties;

    public HentArbeidsgiverToken create(Issuer issuer) {
        switch (issuer) {
            case ISSUER_TOKENX:
                return new HentArbeidsgiverTokenxImpl(oAuth2AccessTokenService, clientConfigurationProperties);
            default:
                throw new RuntimeException();
        }
    }

}
