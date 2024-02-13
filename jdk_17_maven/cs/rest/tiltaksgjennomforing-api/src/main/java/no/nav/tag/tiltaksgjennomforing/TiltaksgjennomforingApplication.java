package no.nav.tag.tiltaksgjennomforing;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJwtTokenValidation(ignore = {
        "org.springdoc",
        "springfox.documentation.swagger.web.ApiResourceController",
        "no.nav.tag.tiltaksgjennomforing.featuretoggles.FeatureToggleController",
        "org.springframework"
})
@EnableConfigurationProperties
@EnableJpaRepositories
@EnableCaching
@OpenAPIDefinition
public class TiltaksgjennomforingApplication {
    public static void main(String[] args) {
        String clusterName = System.getenv("MILJO");
        if (clusterName == null) {
            System.out.println("Kan ikke startes uten miljøvariabel MILJO. Lokalt kan LokalTiltaksgjennomforingApplication kjøres.");
            System.exit(1);
        }
        new SpringApplicationBuilder(TiltaksgjennomforingApplication.class)
                .profiles(clusterName)
                .build()
                .run();
    }
}
