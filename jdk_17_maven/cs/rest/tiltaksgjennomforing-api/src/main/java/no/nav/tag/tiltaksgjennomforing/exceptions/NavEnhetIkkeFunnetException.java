package no.nav.tag.tiltaksgjennomforing.exceptions;

public class NavEnhetIkkeFunnetException extends FeilkodeException {

    public NavEnhetIkkeFunnetException() {
        super(Feilkode.NAV_ENHET_IKKE_FUNNET);
    }
}
