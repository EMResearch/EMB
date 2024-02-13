package no.nav.tag.tiltaksgjennomforing.exceptions;

public class FeilkodeException extends RuntimeException {
    private final Feilkode feilkode;

    public FeilkodeException(Feilkode feilkode) {
        this.feilkode = feilkode;
    }

    public Feilkode getFeilkode() {
        return feilkode;
    }

    @Override
    public String getMessage() {
        return feilkode.name();
    }
}
