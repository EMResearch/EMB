package no.nav.tag.tiltaksgjennomforing.utils;

import lombok.Value;

import java.time.LocalDate;

@Value
public class Periode {
    private final LocalDate start;
    private final LocalDate slutt;

    public Periode(LocalDate start, LocalDate slutt) {
        if (start.isAfter(slutt)) {
            throw new IllegalArgumentException("Startdato må være før eller lik som sluttdato");
        }
        this.start = start;
        this.slutt = slutt;
    }
}
