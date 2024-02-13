package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;

@Value
public class InnloggetMentor implements InnloggetBruker {
    Fnr identifikator;
    Avtalerolle rolle = Avtalerolle.MENTOR;
    boolean erNavAnsatt = false;
}
