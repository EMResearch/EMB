package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Value;

@Value
public class Identer {
    private final String ident;
    private final String gruppe;
    private final boolean historisk;
}
