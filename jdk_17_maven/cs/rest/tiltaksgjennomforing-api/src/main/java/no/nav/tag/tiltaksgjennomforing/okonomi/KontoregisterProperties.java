package no.nav.tag.tiltaksgjennomforing.okonomi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.kontoregister")
public class KontoregisterProperties {
    private String uri;
    private String consumerId;
}
