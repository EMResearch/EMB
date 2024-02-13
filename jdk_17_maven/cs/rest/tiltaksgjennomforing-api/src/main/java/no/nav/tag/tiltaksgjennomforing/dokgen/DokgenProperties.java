package no.nav.tag.tiltaksgjennomforing.dokgen;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.dokgen")
public class DokgenProperties {
    private URI uri;
}
