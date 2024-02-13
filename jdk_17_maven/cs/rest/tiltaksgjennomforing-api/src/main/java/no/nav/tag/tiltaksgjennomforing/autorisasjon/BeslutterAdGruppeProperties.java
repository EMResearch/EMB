package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.beslutter-ad-gruppe")
public class BeslutterAdGruppeProperties {
    private UUID id;
}
