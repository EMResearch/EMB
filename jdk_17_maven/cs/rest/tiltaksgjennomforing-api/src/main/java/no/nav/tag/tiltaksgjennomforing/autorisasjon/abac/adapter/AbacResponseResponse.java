package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.UpperCamelCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(UpperCamelCaseStrategy.class)
public class AbacResponseResponse {
  public String decision;
}
