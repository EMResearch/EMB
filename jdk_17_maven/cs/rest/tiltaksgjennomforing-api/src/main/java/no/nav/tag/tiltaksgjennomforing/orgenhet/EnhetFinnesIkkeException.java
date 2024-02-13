package no.nav.tag.tiltaksgjennomforing.orgenhet;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

public class EnhetFinnesIkkeException extends FeilkodeException {
    public EnhetFinnesIkkeException() {
        super(Feilkode.ENHET_FINNES_IKKE);
    }
}
