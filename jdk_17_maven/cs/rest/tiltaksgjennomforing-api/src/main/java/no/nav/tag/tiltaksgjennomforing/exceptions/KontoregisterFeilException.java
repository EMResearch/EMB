package no.nav.tag.tiltaksgjennomforing.exceptions;

public class KontoregisterFeilException extends FeilkodeException {
    public KontoregisterFeilException() {
        super(Feilkode.KONTOREGISTER_FEIL);
    }
}
