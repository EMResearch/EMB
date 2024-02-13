package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.RefusjonStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class RefusjonEndretStatusMelding {
    String refusjonId;
    String bedriftNr;
    String avtaleId;
    RefusjonStatus status;
    String tilskuddsperiodeId;
}
