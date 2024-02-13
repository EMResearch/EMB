package no.nav.tag.tiltaksgjennomforing.orgenhet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.ereg")
public class EregProperties {
    private URI uri;
}
