package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.axsys")
public class AxsysProperties {
    private URI uri;
    private String navConsumerId;
}