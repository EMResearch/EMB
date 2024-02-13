package no.nav.tag.tiltaksgjennomforing.orgenhet;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;

@Value
public class Organisasjon {
    private final BedriftNr bedriftNr;
    private final String bedriftNavn;
}
