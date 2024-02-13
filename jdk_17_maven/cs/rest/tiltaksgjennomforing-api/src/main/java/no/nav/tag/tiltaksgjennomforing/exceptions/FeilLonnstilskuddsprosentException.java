package no.nav.tag.tiltaksgjennomforing.exceptions;

public class FeilLonnstilskuddsprosentException extends FeilkodeException {

    public FeilLonnstilskuddsprosentException() {
        super(Feilkode.LONNSTILSKUDD_PROSENT_ER_UGYLDIG);
    }
}
