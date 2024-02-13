package no.nav.tag.tiltaksgjennomforing.datavarehus;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.UUID;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.dvh-melding")
public class DvhMeldingProperties {
    private UUID gruppeTilgang;
    private EnumSet<Tiltakstype> tiltakstyper = EnumSet.noneOf(Tiltakstype.class);
}
