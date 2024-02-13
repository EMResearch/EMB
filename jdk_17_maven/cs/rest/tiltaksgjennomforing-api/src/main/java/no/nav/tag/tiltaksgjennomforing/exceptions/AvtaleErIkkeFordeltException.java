package no.nav.tag.tiltaksgjennomforing.exceptions;

public class AvtaleErIkkeFordeltException extends FeilkodeException {
    public AvtaleErIkkeFordeltException() {
        super(Feilkode.IKKE_FORDELT);
    }
}
