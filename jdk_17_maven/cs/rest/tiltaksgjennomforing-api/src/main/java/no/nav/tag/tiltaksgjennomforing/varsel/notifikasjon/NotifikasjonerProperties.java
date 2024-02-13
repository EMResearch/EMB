package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.notifikasjoner")
public class NotifikasjonerProperties {
    private URI uri;
    private String lenke;
}
