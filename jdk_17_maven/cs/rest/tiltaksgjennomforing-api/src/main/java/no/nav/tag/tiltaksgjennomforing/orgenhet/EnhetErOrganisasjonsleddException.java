package no.nav.tag.tiltaksgjennomforing.orgenhet;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

public class EnhetErOrganisasjonsleddException extends FeilkodeException {
    public EnhetErOrganisasjonsleddException() {
        super(Feilkode.ENHET_ER_ORGLEDD);
    }
}