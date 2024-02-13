package no.nav.tag.tiltaksgjennomforing.featuretoggles;


import io.getunleash.strategy.Strategy;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

@Component
public class ByEnvironmentStrategy implements Strategy {

    private final String environment;

    public ByEnvironmentStrategy(@Value("${MILJO:}") String clusterName) {
        this.environment = clusterName.isEmpty() ? Miljø.LOCAL : clusterName;
    }

    @Override
    public String getName() {
        return "byEnvironment";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return Optional.ofNullable(parameters)
                .map(map -> map.get("miljø"))
                .map(env -> asList(env.split(",")).contains(environment))
                .orElse(false);
    }

    String getEnvironment() {
        return environment;
    }
}
