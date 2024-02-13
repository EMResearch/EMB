package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.ArbeidsgiverTokenStrategyFactory;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.HentArbeidsgiverToken;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ArbeidsgiverTokenStrategyFactoryMock implements ArbeidsgiverTokenStrategyFactory {

    @Override
    public HentArbeidsgiverToken create(TokenUtils.Issuer issuer) {
        return () -> "";
    }
}
