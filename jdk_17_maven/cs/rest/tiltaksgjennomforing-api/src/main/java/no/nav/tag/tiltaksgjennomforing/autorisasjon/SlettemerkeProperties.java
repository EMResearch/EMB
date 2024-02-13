package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.slettemerking.tilgang")
public class SlettemerkeProperties {
    private List<NavIdent> ident = new ArrayList<>();

}
