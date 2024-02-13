package no.nav.tag.tiltaksgjennomforing.exceptions;

public class KanIkkeOppheveException extends FeilkodeException {
    public KanIkkeOppheveException() {
        super(Feilkode.KAN_IKKE_OPPHEVE);
    }
}
