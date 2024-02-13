package no.nav.tag.tiltaksgjennomforing.exceptions;

public class KanIkkeOppretteAvtalePåKode6Exception extends FeilkodeException {
    public KanIkkeOppretteAvtalePåKode6Exception() {
        super(Feilkode.IKKE_TILGANG_TIL_DELTAKER);
    }
}
