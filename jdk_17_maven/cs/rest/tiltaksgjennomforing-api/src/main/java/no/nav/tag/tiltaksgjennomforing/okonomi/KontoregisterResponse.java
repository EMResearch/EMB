package no.nav.tag.tiltaksgjennomforing.okonomi;

import lombok.Value;

@Value
public class KontoregisterResponse {
  private final String mottaker;
  private final String kontonr;
  private final String feilmelding;
}
