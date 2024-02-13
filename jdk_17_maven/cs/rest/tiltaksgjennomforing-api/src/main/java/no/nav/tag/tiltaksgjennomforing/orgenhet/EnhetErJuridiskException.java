package no.nav.tag.tiltaksgjennomforing.orgenhet;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

public class EnhetErJuridiskException extends FeilkodeException {
    public EnhetErJuridiskException() {
        super(Feilkode.ENHET_ER_JURIDISK);
    }
}
