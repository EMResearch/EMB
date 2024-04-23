package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;

@Value
public class InnloggetDeltaker implements InnloggetBruker {
    Fnr identifikator;
    Avtalerolle rolle = Avtalerolle.DELTAKER;
    boolean erNavAnsatt = false;
}
