package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Value;

@Value
public class Data {
    private final HentPerson hentPerson;
    private final HentIdenter hentIdenter;
    private final HentGeografiskTilknytning hentGeografiskTilknytning;
}
