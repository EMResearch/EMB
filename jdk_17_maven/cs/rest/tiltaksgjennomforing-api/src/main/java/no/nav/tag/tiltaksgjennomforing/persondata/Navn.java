package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Value;

@Value
public class Navn {
    public static final Navn TOMT_NAVN = new Navn("", "", "");
    private final String fornavn;
    private final String mellomnavn;
    private final String etternavn;
}
