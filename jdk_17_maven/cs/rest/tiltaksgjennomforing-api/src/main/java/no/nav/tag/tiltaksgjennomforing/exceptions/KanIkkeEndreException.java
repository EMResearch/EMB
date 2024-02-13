package no.nav.tag.tiltaksgjennomforing.exceptions;

public class KanIkkeEndreException extends FeilkodeException {
    public KanIkkeEndreException() {
        super(Feilkode.KAN_IKKE_ENDRE);
    }
}
