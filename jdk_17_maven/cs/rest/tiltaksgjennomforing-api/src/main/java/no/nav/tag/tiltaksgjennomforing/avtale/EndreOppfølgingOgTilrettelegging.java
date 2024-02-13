package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class EndreOppfølgingOgTilrettelegging {
    String oppfolging;
    String tilrettelegging;
}