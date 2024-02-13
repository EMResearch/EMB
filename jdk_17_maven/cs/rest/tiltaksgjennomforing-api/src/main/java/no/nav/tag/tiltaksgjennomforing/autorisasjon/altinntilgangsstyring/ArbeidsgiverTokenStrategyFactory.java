package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;

public interface ArbeidsgiverTokenStrategyFactory {
    HentArbeidsgiverToken create(TokenUtils.Issuer issuer);
}
