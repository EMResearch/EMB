package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Value;

@Value
public class Adressebeskyttelse {
    public static final Adressebeskyttelse INGEN_BESKYTTELSE = new Adressebeskyttelse("");
    private final String gradering;
}
