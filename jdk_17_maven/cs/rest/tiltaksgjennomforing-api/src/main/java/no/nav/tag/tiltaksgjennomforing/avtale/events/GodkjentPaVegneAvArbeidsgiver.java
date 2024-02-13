package no.nav.tag.tiltaksgjennomforing.avtale.events;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;

@Value
public class GodkjentPaVegneAvArbeidsgiver {
    Avtale avtale;
    Identifikator utfortAv;
}