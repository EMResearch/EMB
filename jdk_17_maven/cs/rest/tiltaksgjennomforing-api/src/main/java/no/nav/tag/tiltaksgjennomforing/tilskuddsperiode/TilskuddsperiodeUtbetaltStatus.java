package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TilskuddsperiodeUtbetaltStatus {
    UTBETALT("RECONCILED"),
    FEILET("VOIDED");

    private final String status;
}
