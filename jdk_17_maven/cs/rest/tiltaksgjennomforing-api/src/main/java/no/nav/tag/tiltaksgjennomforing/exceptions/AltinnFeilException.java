package no.nav.tag.tiltaksgjennomforing.exceptions;

public class AltinnFeilException extends FeilkodeException {
    public AltinnFeilException() {
        super(Feilkode.ALTINN_FEIL);
    }
}
