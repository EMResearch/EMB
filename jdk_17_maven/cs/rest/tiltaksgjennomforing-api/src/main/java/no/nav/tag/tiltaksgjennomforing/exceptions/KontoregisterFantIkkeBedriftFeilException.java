package no.nav.tag.tiltaksgjennomforing.exceptions;

public class KontoregisterFantIkkeBedriftFeilException extends FeilkodeException {
    public KontoregisterFantIkkeBedriftFeilException() {
        super(Feilkode.KONTOREGISTER_FEIL_BEDRIFT_IKKE_FUNNET);
    }
}
