package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response;

import lombok.Value;

@Value
public class FellesResponse {
    String __typename;
    String id;
    String feilmelding;
}
