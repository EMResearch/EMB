package no.nav.tag.tiltaksgjennomforing.avtale.events;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriode;

@Value
public class TilskuddsperiodeGodkjent {
    Avtale avtale;
    TilskuddPeriode tilskuddsperiode;
    Identifikator utfortAv;
    Integer resendingsnummer;
}
