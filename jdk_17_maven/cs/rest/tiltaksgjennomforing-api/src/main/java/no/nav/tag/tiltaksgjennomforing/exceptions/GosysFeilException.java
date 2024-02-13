package no.nav.tag.tiltaksgjennomforing.exceptions;

public class GosysFeilException extends FeilkodeException {
    public GosysFeilException() {
        super(Feilkode.GOSYS_FEIL);
    }
}
