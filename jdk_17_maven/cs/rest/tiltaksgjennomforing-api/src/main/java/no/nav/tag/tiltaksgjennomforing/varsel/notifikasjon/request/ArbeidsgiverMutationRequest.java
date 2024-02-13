package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.request;


import lombok.Value;

@Value
public class ArbeidsgiverMutationRequest {
    String query;
    Variables variables;
}
