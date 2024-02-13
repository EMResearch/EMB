package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.altinn-tilgangsstyring")
public class AltinnTilgangsstyringProperties {
    private URI uri;
    private URI proxyUri;
    private String altinnApiKey;
    private String apiGwApiKey;
    private String beOmRettighetBaseUrl;
    private Integer ltsMidlertidigServiceCode;
    private Integer ltsMidlertidigServiceEdition;
    private Integer ltsVarigServiceCode;
    private Integer ltsVarigServiceEdition;
    private Integer arbtreningServiceCode;
    private Integer arbtreningServiceEdition;
    private Integer sommerjobbServiceCode;
    private Integer sommerjobbServiceEdition;
    private Integer inkluderingstilskuddServiceCode;
    private Integer inkluderingstilskuddServiceEdition;
    private Integer mentorServiceCode;
    private Integer mentorServiceEdition;
}
