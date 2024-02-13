package no.nav.tag.tiltaksgjennomforing.featuretoggles;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeatureToggleService {

    private final Unleash unleash;
    private final TokenUtils tokenUtils;

    @Autowired
    public FeatureToggleService(Unleash unleash, TokenUtils tokenUtils) {
        this.unleash = unleash;
        this.tokenUtils = tokenUtils;
    }

    public Map<String, Boolean> hentFeatureToggles(List<String> features) {

        return features.stream().collect(Collectors.toMap(
                feature -> feature,
                feature -> isEnabled(feature)
        ));
    }

    public Map<String, Variant> hentVarianter(List<String> features) {

        return features.stream().collect(Collectors.toMap(
                feature -> feature,
                feature -> unleash.getVariant(feature, contextMedInnloggetBruker())
        ));
    }


    public Boolean isEnabled(String feature) {
        return unleash.isEnabled(feature, contextMedInnloggetBruker());
    }

    private UnleashContext contextMedInnloggetBruker() {
        UnleashContext.Builder builder = UnleashContext.builder();
        tokenUtils.hentBrukerOgIssuer().map(a -> builder.userId(a.getBrukerIdent()));
        return builder.build();
    }

}
