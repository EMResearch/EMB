package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.FeilVedSendingResponse;

import lombok.Value;

@Value
public class FeilVedSendingResponse {
    Errors[] errors;
}
