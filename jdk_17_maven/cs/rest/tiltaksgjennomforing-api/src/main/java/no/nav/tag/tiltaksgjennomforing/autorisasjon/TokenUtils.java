package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils.Issuer.*;

@Component
@RequiredArgsConstructor
public class TokenUtils {
    private static final String ACR = "acr";
    private static final String LEVEL4 = "Level4";

    public UUID hentAzureOid() {
        return hentClaim(ISSUER_AAD, "oid").map(UUID::fromString).orElse(null);
    }

    public enum Issuer {
        ISSUER_AAD("aad"),
        ISSUER_SYSTEM("system"),
        ISSUER_TOKENX("tokenx");

        final String issuerName;

        Issuer(String issuerName) {
            this.issuerName = issuerName;
        }
    }

    @Value
    public static class BrukerOgIssuer {
        Issuer issuer;
        String brukerIdent;
    }

    private final TokenValidationContextHolder contextHolder;

    public Optional<BrukerOgIssuer> hentBrukerOgIssuer() {
        return hentClaim(ISSUER_SYSTEM, "sub").map(sub -> new BrukerOgIssuer(ISSUER_SYSTEM, sub))
            .or(() -> hentClaim(ISSUER_AAD, "NAVident").map(sub -> new BrukerOgIssuer(ISSUER_AAD, sub)))
            .or(() -> hentClaim(ISSUER_TOKENX, "pid").map(it -> new BrukerOgIssuer(ISSUER_TOKENX, it)));
    }

    public boolean harAdGruppe(UUID gruppeAD) {
        Optional<List<String>> groupsClaim = hentClaims(ISSUER_AAD, "groups");
        if (!groupsClaim.isPresent()) {
            return false;
        }
        return groupsClaim.get().contains(gruppeAD.toString());
    }

    public boolean harAdRolle(String rolle) {
        Optional<List<String>> roller = hentClaims(ISSUER_AAD, "roles");
        if (!roller.isPresent()) {
            return false;
        }
        return roller.get().contains(rolle.toString());
    }

    private Optional<List<String>> hentClaims(Issuer issuer, String claim) {
        return hentClaimSet(issuer).filter(jwtClaimsSet -> innloggingsNivaOK(issuer, jwtClaimsSet))
                .map(jwtClaimsSet -> (List<String>) jwtClaimsSet.get(claim));
    }

    private Optional<String> hentClaim(Issuer issuer, String claim) {
        return hentClaimSet(issuer)
                .filter(jwtClaimsSet -> innloggingsNivaOK(issuer, jwtClaimsSet))
                .map(jwtClaimsSet -> jwtClaimsSet.get(claim))
                .map(String::valueOf);
    }

    private boolean innloggingsNivaOK(Issuer issuer, JwtTokenClaims jwtClaimsSet) {

        return issuer != ISSUER_TOKENX || LEVEL4.equals(jwtClaimsSet.get(ACR));
    }

    private Optional<JwtTokenClaims> hentClaimSet(Issuer issuer) {
        TokenValidationContext tokenValidationContext;
        try {
            tokenValidationContext = contextHolder.getTokenValidationContext();
        } catch (IllegalStateException e) {
            // Er ikke i kontekst av en request
            return Optional.empty();
        }
        JwtTokenClaims claims = tokenValidationContext.getClaims(issuer.issuerName);
        return Optional.ofNullable(claims);

    }

    public String hentTokenx() {
        return contextHolder.getTokenValidationContext().getJwtToken(ISSUER_TOKENX.issuerName).getTokenAsString();
    }

}
