package no.nav.tag.tiltaksgjennomforing.enhet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tiltaksgjennomforing.norg2.geografisk")
public class Norg2GeografiskProperties {
    private String url;
}
