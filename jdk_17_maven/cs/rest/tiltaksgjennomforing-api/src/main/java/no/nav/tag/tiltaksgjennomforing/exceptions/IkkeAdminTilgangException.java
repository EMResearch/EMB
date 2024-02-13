package no.nav.tag.tiltaksgjennomforing.exceptions;

public class IkkeAdminTilgangException extends FeilkodeException{
    public IkkeAdminTilgangException() {
        super(Feilkode.IKKE_ADMIN_TILGANG);
    }
}
