package no.nav.tag.tiltaksgjennomforing.exceptions;

public class TiltaksgjennomforingException extends RuntimeException {

    public TiltaksgjennomforingException(String message) {
        super(message);
    }

    public TiltaksgjennomforingException(String message, Throwable cause) {
        super(message, cause);
    }
}
