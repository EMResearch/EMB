package no.nav.tag.tiltaksgjennomforing.featuretoggles;


import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.ByEnhetStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletRequest;


@Configuration
public class FeatureToggleConfig {

    private static final String APP_NAME = "tiltaksgjennomforing-api";

    @Bean
    @ConditionalOnProperty("tiltaksgjennomforing.unleash.enabled")
    public Unleash initializeUnleash(
            @Value("${tiltaksgjennomforing.unleash.api-uri}") String unleashUrl,
            @Value("${tiltaksgjennomforing.unleash.api-token}") String apiKey,
                                     ByEnvironmentStrategy byEnvironmentStrategy,
                                     ByEnhetStrategy byEnhetStrategy,
                                     ByOrgnummerStrategy byOrgnummerStrategy) {
        UnleashConfig config = UnleashConfig.builder()
                .appName(APP_NAME)
                .instanceId(APP_NAME + "-" + byEnvironmentStrategy.getEnvironment())
                .unleashAPI(unleashUrl)
                .apiKey(apiKey)
                .build();

        return new DefaultUnleash(
                config,
                byEnvironmentStrategy,
                byEnhetStrategy,
                byOrgnummerStrategy
        );
    }

    @Bean
    @ConditionalOnProperty("tiltaksgjennomforing.unleash.mock")
    @RequestScope
    public Unleash unleashMock(@Autowired HttpServletRequest request) {
        FakeFakeUnleash fakeUnleash = new FakeFakeUnleash();
        boolean allEnabled = "enabled".equals(request.getHeader("features"));
        if (allEnabled) {
            fakeUnleash.enableAll();
        } else {
            fakeUnleash.disableAll();
        }
        return fakeUnleash;
    }
}
