package no.nav.tag.tiltaksgjennomforing.avtale.events;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;

@Value
public class GodkjenningerOpphevetAvVeileder {
    Avtale avtale;
    GamleVerdier gamleVerdier;
}
