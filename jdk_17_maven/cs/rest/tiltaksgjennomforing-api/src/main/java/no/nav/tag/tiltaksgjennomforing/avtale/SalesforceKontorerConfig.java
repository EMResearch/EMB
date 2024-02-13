package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.salesforcekontorer")
public class SalesforceKontorerConfig {
    private List<String> enheter = Collections.emptyList();
}
