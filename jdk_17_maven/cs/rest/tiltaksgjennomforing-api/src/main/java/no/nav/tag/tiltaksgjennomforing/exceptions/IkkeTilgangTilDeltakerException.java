package no.nav.tag.tiltaksgjennomforing.exceptions;

public class IkkeTilgangTilDeltakerException extends FeilkodeException {
    public IkkeTilgangTilDeltakerException() {
        super(Feilkode.IKKE_TILGANG_TIL_DELTAKER);
    }
}
