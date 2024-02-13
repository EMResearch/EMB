package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.persondata")
public class PersondataProperties {
    private URI uri;
}
