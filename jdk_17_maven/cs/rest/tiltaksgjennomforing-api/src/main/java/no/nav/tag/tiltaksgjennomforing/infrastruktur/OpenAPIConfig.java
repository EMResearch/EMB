package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    private OpenAPI getInfo(OpenAPI openAPI) {
        return openAPI
                .info(new Info().title("Tiltaksgjennomføring API")
                        .license(new License()
                                        .name("MIT License")
                                        .url("https://github.com/navikt/tiltaksgjennomforing-api/blob/master/LICENSE.md"))
                ).externalDocs(
                        new ExternalDocumentation()
                                .description("Avtaleløsning for arbeidstiltak.")
                                .url("https://github.com/navikt/tiltaksgjennomforing-api"));
    }

    @Bean
    public OpenAPI TiltakOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        OpenAPI openAPI = new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName, new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
                return getInfo(openAPI);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("tiltaksgjennomforing-api")
                .pathsToMatch("/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                    return operation;
                })
                .addOpenApiCustomiser(this::getInfo).build();
    }
}
